package com.jetbrains.edu.learning.serialization.converter.json

import com.google.gson.JsonObject
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.*

class ToSixthVersionJsonStepOptionConverter : JsonStepOptionsConverter {

  override fun convert(stepOptionsJson: JsonObject): JsonObject {
    val additionalFiles = stepOptionsJson.remove(ADDITIONAL_FILES)?.asJsonArray
    val additionalFilesMap = JsonObject()
    if (additionalFiles != null) {
      for (element in additionalFiles) {
        val additionalFile = element.asJsonObject
        val path = additionalFile.getAsJsonPrimitive(NAME).asString
        val text = additionalFile.getAsJsonPrimitive(TEXT).asString
        val newAdditionalFile = JsonObject().apply {
          addProperty(TEXT, text)
          addProperty(IS_VISIBLE, true)
        }
        additionalFilesMap.add(path, newAdditionalFile)
      }
    }
    stepOptionsJson.add(ADDITIONAL_FILES, additionalFilesMap)
    return stepOptionsJson
  }
}
