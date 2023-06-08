package com.jetbrains.edu.learning.serialization.converter.json

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intellij.openapi.editor.EditorFactory
import com.jetbrains.edu.learning.serialization.SerializationUtils
import java.io.IOException

class ToSecondVersionJsonStepOptionsConverter : JsonStepOptionsConverter {
  override fun convert(stepOptionsJson: ObjectNode): ObjectNode {
    val files = stepOptionsJson[SerializationUtils.Json.FILES]
    if (files != null) {
      for (taskFileElement in files) {
        val placeholders = taskFileElement[SerializationUtils.Json.PLACEHOLDERS]
        for (placeholder in placeholders) {
          convertToAbsoluteOffset(taskFileElement, placeholder as ObjectNode)
          convertMultipleHints(placeholder)
          convertToSubtaskInfo(placeholder)
        }
      }
    }
    return stepOptionsJson
  }

  companion object {
    private fun convertToAbsoluteOffset(taskFileObject: JsonNode, placeholderObject: ObjectNode) {
      val line = placeholderObject[SerializationUtils.LINE].asInt()
      val start = placeholderObject[SerializationUtils.START].asInt()
      if (line == -1) {
        placeholderObject.put(SerializationUtils.OFFSET, start)
      }
      else {
        val document = EditorFactory.getInstance().createDocument(taskFileObject[SerializationUtils.Json.TEXT].asText())
        placeholderObject.put(SerializationUtils.OFFSET, document.getLineStartOffset(line) + start)
      }
    }

    private fun convertMultipleHints(placeholderObject: ObjectNode) {
      val hintString = placeholderObject[SerializationUtils.HINT].asText()
      val hintsArray = placeholderObject.putArray(SerializationUtils.ADDITIONAL_HINTS)
      try {
        val hints = ObjectMapper().readValue(hintString, object : TypeReference<List<String>>() {})
        if (hints != null && hints.isNotEmpty()) {
          for (i in hints.indices) {
            if (i == 0) {
              placeholderObject.put(SerializationUtils.HINT, hints[0])
              continue
            }
            hintsArray.add(hints[i])
          }
        }
        else {
          placeholderObject.put(SerializationUtils.HINT, "")
        }
      }
      catch (e: JsonMappingException) {
        hintsArray.add(hintString)
      }
      catch (e: IOException) {
        hintsArray.add(hintString)
      }
    }

    private fun convertToSubtaskInfo(placeholderObject: ObjectNode) {
      val subtaskInfo = ObjectMapper().createObjectNode()
      val subtaskInfos = placeholderObject.putArray(SerializationUtils.Json.SUBTASK_INFOS)
      val hintsArray = subtaskInfo.putArray(SerializationUtils.Json.HINTS)
      hintsArray.add(placeholderObject[SerializationUtils.HINT].asText())
      val additionalHints = placeholderObject[SerializationUtils.ADDITIONAL_HINTS]
      if (additionalHints != null) {
        for (hint in additionalHints) {
          hintsArray.add(hint)
        }
      }
      subtaskInfos.add(subtaskInfo)
      subtaskInfo.put(SerializationUtils.Json.INDEX, 0)
      subtaskInfo.put(SerializationUtils.Json.POSSIBLE_ANSWER, placeholderObject[SerializationUtils.Json.POSSIBLE_ANSWER].asText())
    }
  }
}
