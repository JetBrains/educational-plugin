package com.jetbrains.edu.learning.serialization.converter.json

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.*
import com.jetbrains.edu.learning.serialization.SerializationUtils.STATUS

class ToFifthVersionJsonStepOptionsConverter : JsonStepOptionsConverter {

  override fun convert(stepOptionsJson: ObjectNode): ObjectNode {
    val taskFiles = stepOptionsJson.get(FILES)
    if (taskFiles != null) {
      for (file in taskFiles) {
        for (placeholder in file.get(PLACEHOLDERS)) {
          removeSubtaskInfo(placeholder as ObjectNode)
        }
      }
    }
    migrateDescription(stepOptionsJson)
    return stepOptionsJson
  }

  companion object {

    private val LOG: Logger = Logger.getInstance(ToFifthVersionJsonStepOptionsConverter::class.java)

    @JvmStatic
    fun removeSubtaskInfo(placeholderObject: ObjectNode) {
      val subtaskInfos = placeholderObject.get(SUBTASK_INFOS)
      if (!subtaskInfos.isArray) return
      val info = when (subtaskInfos.size) {
        0 -> {
          LOG.warn("Can't find subtask info object")
          return
        }
        1 -> subtaskInfos.firstValue
        else -> {
          LOG.warn(String.format("Placeholder contains %d subtask info objects. Expected: 1", subtaskInfos.size))
          subtaskInfos.firstValue
        }
      }

      placeholderObject.put(POSSIBLE_ANSWER, info.get(POSSIBLE_ANSWER).asText())
      placeholderObject.set(HINTS, info.get(HINTS))
      val placeholderText = info.get(PLACEHOLDER_TEXT)
      if (placeholderText != null) {
        placeholderObject.put(PLACEHOLDER_TEXT, placeholderText.asText())
      }
      val status = info.get(STATUS)
      if (status != null) {
        placeholderObject.put(STATUS, status.asText())
      }
      placeholderObject.remove(SUBTASK_INFOS)
    }

    private fun migrateDescription(stepOptions: ObjectNode) {
      val taskTexts = stepOptions.get(TEXTS)
      if (taskTexts != null && taskTexts.size() > 0) {
        val description = taskTexts.get(0).get(FILE_WRAPPER_TEXT).asText()
        stepOptions.put(DESCRIPTION_TEXT, description)
      }
      stepOptions.put(DESCRIPTION_FORMAT, DescriptionFormat.HTML.toString())
      stepOptions.remove(TEXTS)
    }

    private val JsonNode.size: Int get() = when {
      isArray -> (this as ArrayNode).size()
      isObject -> (this as ObjectNode).size()
      else -> error("Unsupported json element type")
    }

    private val JsonNode.firstValue: JsonNode get() = when {
      isArray -> get(0)
      isObject -> (this as ObjectNode).elements().next()
      else -> error("Unsupported json element type")
    }
  }
}
