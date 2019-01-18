package com.jetbrains.edu.learning.stepik.hyperskill.serialization

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intellij.openapi.util.text.StringUtil
import com.jetbrains.edu.learning.JSON_FORMAT_VERSION
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.serialization.SerializationUtils
import com.jetbrains.edu.learning.stepik.StepOptions

class JacksonStepOptionsDeserializer @JvmOverloads constructor(vc: Class<*>? = null) : StdDeserializer<StepOptions>(vc) {

  override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): StepOptions {
    val node: JsonNode = jp.codec.readTree(jp)
    migrate(node)
    return objectMapper().treeToValue(node, StepOptions::class.java)
  }

  private fun objectMapper(): ObjectMapper {
    val module = SimpleModule()
    module.addDeserializer(TaskFile::class.java, JacksonTaskFileDeserializer())
    val objectMapper = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    objectMapper.registerModule(module)
    return objectMapper
  }

  private fun migrate(node: JsonNode) {
    val versionJson = node.get(SerializationUtils.Json.FORMAT_VERSION)
    val maxVersion = JSON_FORMAT_VERSION

    var version = 1
    if (versionJson != null) {
      version = versionJson.asInt()
    }
    while (version < maxVersion) {
      when (version) {
        8 -> convertTo9Version(node)
        else -> {}
      }
      version++
    }
    (node as ObjectNode).put(SerializationUtils.Json.FORMAT_VERSION, maxVersion)
  }

  private fun convertTo9Version(stepOptionsJson: JsonNode) {
    val taskFiles = stepOptionsJson.get(SerializationUtils.Json.FILES) as ArrayNode
    val testFiles = (stepOptionsJson as ObjectNode).remove(SerializationUtils.Json.TESTS) ?: emptyList<JsonNode>()
    for (testFile in testFiles) {
      if (testFile !is ObjectNode) continue
      testFile.put(SerializationUtils.Json.IS_VISIBLE, false)
      taskFiles.add(testFile)
    }

    val additionalFiles = stepOptionsJson.remove(SerializationUtils.Json.ADDITIONAL_FILES) as? ObjectNode
    if (additionalFiles != null) {
      for ((path, additionalFile) in additionalFiles.fields()) {
        if (additionalFile !is ObjectNode) continue
        additionalFile.put(SerializationUtils.Json.NAME, path)
        taskFiles.add(additionalFile)
      }
    }
  }
}

class JacksonTaskFileDeserializer @JvmOverloads constructor(vc: Class<*>? = null) : StdDeserializer<TaskFile>(vc) {

  override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): TaskFile {
    val node: JsonNode = jp.codec.readTree(jp)
    val objectMapper = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    val taskFile = objectMapper.treeToValue(node, TaskFile::class.java)
    taskFile.setText(StringUtil.convertLineSeparators(taskFile.text))
    return taskFile
  }
}
