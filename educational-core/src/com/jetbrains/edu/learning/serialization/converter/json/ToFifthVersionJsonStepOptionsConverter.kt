package com.jetbrains.edu.learning.serialization.converter.json

import com.google.gson.JsonObject
import com.jetbrains.edu.learning.serialization.SerializationUtils
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.FILES
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.PLACEHOLDERS

class ToFifthVersionJsonStepOptionsConverter : JsonStepOptionsConverter {

  override fun convert(stepOptionsJson: JsonObject): JsonObject {
    for (file in stepOptionsJson.getAsJsonArray(FILES)) {
      for (placeholder in file.asJsonObject.getAsJsonArray(PLACEHOLDERS)) {
        SerializationUtils.Json.removeSubtaskInfo(placeholder.asJsonObject)
      }
    }
    return stepOptionsJson
  }
}
