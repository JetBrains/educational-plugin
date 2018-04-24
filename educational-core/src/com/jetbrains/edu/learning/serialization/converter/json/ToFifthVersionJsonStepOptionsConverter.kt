package com.jetbrains.edu.learning.serialization.converter.json

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.*
import com.jetbrains.edu.learning.serialization.SerializationUtils.STATUS

class ToFifthVersionJsonStepOptionsConverter : JsonStepOptionsConverter {

  override fun convert(stepOptionsJson: JsonObject): JsonObject {
    val taskFiles = stepOptionsJson.getAsJsonArray(FILES)
    if (taskFiles != null) {
      for (file in taskFiles) {
        for (placeholder in file.asJsonObject.getAsJsonArray(PLACEHOLDERS)) {
          removeSubtaskInfo(placeholder.asJsonObject)
        }
      }
    }
    migrateDescription(stepOptionsJson)
    return stepOptionsJson
  }

  companion object {

    private val LOG: Logger = Logger.getInstance(ToFifthVersionJsonStepOptionsConverter::class.java)

    @JvmStatic
    fun removeSubtaskInfo(placeholderObject: JsonObject): JsonObject {
      val subtaskInfos = placeholderObject.get(SUBTASK_INFOS)
      val info = when (subtaskInfos.size) {
        0 -> {
          LOG.warn("Can't find subtask info object")
          return placeholderObject
        }
        1 -> subtaskInfos.firstValue
        else -> {
          LOG.warn(String.format("Placeholder contains %d subtask info objects. Expected: 1", subtaskInfos.size))
          subtaskInfos.firstValue
        }
      }

      placeholderObject.addProperty(POSSIBLE_ANSWER, info.getAsJsonPrimitive(POSSIBLE_ANSWER).asString)
      placeholderObject.add(HINTS, info.getAsJsonArray(HINTS))
      val placeholderText = info.getAsJsonPrimitive(PLACEHOLDER_TEXT)
      if (placeholderText != null) {
        placeholderObject.addProperty(PLACEHOLDER_TEXT, placeholderText.asString)
      }
      val status = info.getAsJsonPrimitive(STATUS)
      if (status != null) {
        placeholderObject.addProperty(STATUS, status.asString)
      }
      placeholderObject.remove(SUBTASK_INFOS)
      return placeholderObject
    }

    private fun migrateDescription(stepOptions: JsonObject) {
      val taskTexts = stepOptions.getAsJsonArray(TEXTS)
      if (taskTexts != null && taskTexts.size() > 0) {
        val description = taskTexts.get(0).asJsonObject.getAsJsonPrimitive(FILE_WRAPPER_TEXT).asString
        stepOptions.addProperty(DESCRIPTION_TEXT, description)
      }
      stepOptions.addProperty(DESCRIPTION_FORMAT, DescriptionFormat.HTML.toString().toLowerCase())
      stepOptions.remove(TEXTS)
    }

    private val JsonElement.size: Int get() = when {
      isJsonArray -> asJsonArray.size()
      isJsonObject -> asJsonObject.size()
      else -> error("Unsupported json element type")
    }

    private val JsonElement.firstValue: JsonObject get() = when {
      isJsonArray -> asJsonArray.get(0).asJsonObject
      isJsonObject -> asJsonObject.entrySet().first().value.asJsonObject
      else -> error("Unsupported json element type")
    }
  }
}
