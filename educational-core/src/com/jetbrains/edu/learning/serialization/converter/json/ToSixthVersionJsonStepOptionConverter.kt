package com.jetbrains.edu.learning.serialization.converter.json

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.*

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
        additionalFilesMap.set(path, newAdditionalFile)
      }
    }
    stepOptionsJson.set(ADDITIONAL_FILES, additionalFilesMap)
    return stepOptionsJson
  }
}
