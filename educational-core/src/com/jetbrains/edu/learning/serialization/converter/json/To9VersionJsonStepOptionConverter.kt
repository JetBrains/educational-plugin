package com.jetbrains.edu.learning.serialization.converter.json

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.*

class To9VersionJsonStepOptionConverter : JsonStepOptionsConverter {
  override fun convert(stepOptionsJson: JsonObject): JsonObject {
    val taskFiles = stepOptionsJson.getAsJsonArray(FILES)
    val testFiles = stepOptionsJson.remove(TESTS)?.asJsonArray ?: emptyList<JsonElement>()
    for (testFile in testFiles) {
      if (testFile !is JsonObject) continue
      testFile.addProperty(IS_VISIBLE, false)
      taskFiles.add(testFile)
    }

    val additionalFiles = stepOptionsJson.remove(ADDITIONAL_FILES)?.asJsonObject
    for ((path, additionalFile) in additionalFiles?.entrySet().orEmpty()) {
      if (additionalFile !is JsonObject) continue
      additionalFile.addProperty(NAME, path)
      taskFiles.add(additionalFile)
    }

    return stepOptionsJson
  }
}
