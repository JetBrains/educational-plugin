package com.jetbrains.edu.learning.serialization.converter.json

import com.google.gson.JsonObject
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.*

class ToFifthVersionJsonStepOptionsConverter : JsonStepOptionsConverter {

  override fun convert(stepOptionsJson: JsonObject): JsonObject {
    for (file in stepOptionsJson.getAsJsonArray(FILES)) {
      for (placeholder in file.asJsonObject.getAsJsonArray(PLACEHOLDERS)) {
        val placeholderObject = placeholder.asJsonObject
        val info = placeholderObject.getAsJsonArray(SUBTASK_INFOS)
                     .firstOrNull()
                     ?.asJsonObject
                   ?: error("Can't find subtask info")
        placeholderObject.addProperty(PLACEHOLDER_TEXT, info.getAsJsonPrimitive(PLACEHOLDER_TEXT)?.asString)
        placeholderObject.addProperty(POSSIBLE_ANSWER, info.getAsJsonPrimitive(POSSIBLE_ANSWER).asString)
        placeholderObject.add(HINTS, info.getAsJsonArray(HINTS))
        placeholderObject.remove(SUBTASK_INFOS)
      }
    }
    return stepOptionsJson
  }
}
