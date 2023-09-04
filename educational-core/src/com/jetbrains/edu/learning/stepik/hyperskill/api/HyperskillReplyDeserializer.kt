package com.jetbrains.edu.learning.stepik.hyperskill.api

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.edu.learning.serialization.SerializationUtils
import com.jetbrains.edu.learning.stepik.api.*
import com.jetbrains.rd.util.first

class HyperskillReplyDeserializer(vc: Class<*>? = null) : StdDeserializer<Reply>(vc) {
  override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): Reply {
    val jsonObject: ObjectNode = jp.codec.readTree(jp) as ObjectNode
    val type = jsonObject.tryGuessType()
    val objectMapper = StepikBasedConnector.createObjectMapper(SimpleModule())
    return objectMapper.treeToValue(jsonObject, type)
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(StepikReplyDeserializer::class.java)

    private fun ObjectNode.tryGuessType(): Class<out Reply> {
      val typesToImportantField = mapOf(
        CodeTaskReply::class.java to CODE,
        EduTaskReply::class.java to SOLUTION,
        ChoiceTaskReply::class.java to CHOICES,
        SortingBasedTaskReply::class.java to ORDERING,
        DataTaskReply::class.java to FILE,
        NumberTaskReply::class.java to NUMBER,
        TextTaskReply::class.java to SerializationUtils.Json.TEXT,
      )
      val possibleTypes = typesToImportantField.filter { get(it.value)?.isNull == false }

      if (possibleTypes.size != 1) {
        LOG.error("Could not guess type of reply")
        return Reply::class.java
      }

      return possibleTypes.first().key
    }
  }
}