package com.jetbrains.edu.learning.stepik.hyperskill.api

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.edu.learning.courseFormat.JSON_FORMAT_VERSION
import com.jetbrains.edu.learning.serialization.SerializationUtils
import com.jetbrains.edu.learning.stepik.api.*
import com.jetbrains.rd.util.first

class HyperskillReplyDeserializer(vc: Class<*>? = null) : StdDeserializer<Reply>(vc) {
  override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): Reply {
    val jsonObject: ObjectNode = jp.codec.readTree(jp) as ObjectNode
    jsonObject.migrate(JSON_FORMAT_VERSION)
    val objectMapper = StepikBasedConnector.createObjectMapper(SimpleModule())
    return objectMapper.treeToValue(jsonObject, Reply::class.java)
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(StepikReplyDeserializer::class.java)

    @VisibleForTesting
    fun ObjectNode.migrate(maxVersion: Int): Int {
      val versionJson = get(SerializationUtils.Json.VERSION)
      val initialVersion = versionJson?.asInt() ?: 1
      var version = initialVersion
      while (version < maxVersion) {
        when (version) {
          16 -> toSeventeenthVersion()
        }
        version++
      }
      put(SerializationUtils.Json.VERSION, maxVersion)
      return initialVersion
    }

    private fun ObjectNode.toSeventeenthVersion() {
      if (get("type") != null) return

      val typesToImportantField = mapOf(
        CODE_TASK to CODE,
        EDU_TASK to SOLUTION,
        CHOICE_TASK to CHOICES,
        SORTING_BASED_TASK to ORDERING,
        DATA_TASK to FILE,
        NUMBER_TASK to NUMBER,
        STRING_TASK to SerializationUtils.Json.TEXT,
      )
      val possibleTypes = typesToImportantField.filter { get(it.value)?.isNull == false }

      if (possibleTypes.size != 1) {
        LOG.error("Could not guess type of reply during migration to 17 API version")
        return
      }

      put(TYPE, possibleTypes.first().key)
    }
  }
}