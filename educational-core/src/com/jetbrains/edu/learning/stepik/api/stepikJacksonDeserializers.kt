package com.jetbrains.edu.learning.stepik.api

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.common.annotations.VisibleForTesting
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.JSON_FORMAT_VERSION
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.serialization.SerializationUtils
import com.jetbrains.edu.learning.serialization.converter.json.*
import com.jetbrains.edu.learning.stepik.StepOptions
import com.jetbrains.edu.learning.stepik.StepikNames

class JacksonLessonDeserializer @JvmOverloads constructor(vc: Class<*>? = null) : StdDeserializer<Lesson>(vc) {

  override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): Lesson {
    val node: JsonNode = jp.codec.readTree(jp)
    val objectMapper = StepikConnector.getMapper(SimpleModule())
    val lesson = objectMapper.treeToValue(node, Lesson::class.java)
    val name = lesson.name
    if (StepikNames.PYCHARM_ADDITIONAL == name) {
      lesson.name = EduNames.ADDITIONAL_MATERIALS
    }
    return lesson
  }
}

class JacksonStepOptionsDeserializer @JvmOverloads constructor(var language: String,
                                                               vc: Class<*>? = null) : StdDeserializer<StepOptions>(vc) {

  override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): StepOptions {
    val objectMapper = StepikConnector.getMapper(SimpleModule())
    val node: JsonNode = jp.codec.readTree(jp)
    val migratedNode = migrate(node as ObjectNode, JSON_FORMAT_VERSION, language)
    return objectMapper.treeToValue(migratedNode, StepOptions::class.java)
  }

  companion object {
    @VisibleForTesting
    @JvmOverloads
    @JvmStatic
    fun migrate(node: ObjectNode, maxVersion: Int, language: String? = null): ObjectNode {
      var convertedStepOptions = node
      val versionJson = node.get(SerializationUtils.Json.FORMAT_VERSION)
      var version = 1
      if (versionJson != null) {
        version = versionJson.asInt()
      }
      while (version < maxVersion) {
        val converter = when (version) {
          1 -> ToSecondVersionJsonStepOptionsConverter()
          2 -> ToThirdVersionJsonStepOptionsConverter()
          3 -> ToFourthVersionJsonStepOptionsConverter()
          4 -> ToFifthVersionJsonStepOptionsConverter()
          5 -> ToSixthVersionJsonStepOptionConverter()
          6 -> ToSeventhVersionJsonStepOptionConverter(language)
          8 -> To9VersionJsonStepOptionConverter()
          else -> null
        }
        if (converter != null) {
          convertedStepOptions = converter.convert(convertedStepOptions)
        }
        version++
      }
      node.put(SerializationUtils.Json.FORMAT_VERSION, maxVersion)
      return convertedStepOptions
    }
  }
}
