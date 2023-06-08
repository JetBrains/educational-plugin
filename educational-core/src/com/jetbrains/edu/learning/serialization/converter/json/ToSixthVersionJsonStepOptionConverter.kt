package com.jetbrains.edu.learning.serialization.converter.json

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.ADDITIONAL_FILES
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.IS_VISIBLE
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.NAME
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.TEXT

class ToSixthVersionJsonStepOptionConverter : JsonStepOptionsConverter {

  override fun convert(stepOptionsJson: ObjectNode): ObjectNode {
    val additionalFiles = stepOptionsJson.remove(ADDITIONAL_FILES)
    val additionalFilesMap = ObjectMapper().createObjectNode()
    if (additionalFiles != null) {
      for (additionalFile in additionalFiles) {
        val path = additionalFile.get(NAME).asText()
        val text = additionalFile.get(TEXT).asText()
        val newAdditionalFile = ObjectMapper().createObjectNode()
        newAdditionalFile.put(TEXT, text)
        newAdditionalFile.put(IS_VISIBLE, true)
        additionalFilesMap.set<JsonNode?>(path, newAdditionalFile)
      }
    }
    stepOptionsJson.set<JsonNode?>(ADDITIONAL_FILES, additionalFilesMap)
    return stepOptionsJson
  }
}
