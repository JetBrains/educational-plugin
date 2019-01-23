package com.jetbrains.edu.learning.serialization.converter.json

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.*

class To9VersionJsonStepOptionConverter : JsonStepOptionsConverter {
  override fun convert(stepOptionsJson: ObjectNode): ObjectNode {
    val taskFiles = stepOptionsJson.get(FILES) as? ArrayNode ?: ObjectMapper().createArrayNode()
    val testFiles = stepOptionsJson.remove(TESTS) ?: emptyList<JsonNode>()
    for (testFile in testFiles) {
      if (testFile !is ObjectNode) continue
      testFile.put(IS_VISIBLE, false)
      taskFiles.add(testFile)
    }

    val additionalFiles = stepOptionsJson.remove(ADDITIONAL_FILES)
    if (additionalFiles == null) return stepOptionsJson
    for ((path, additionalFile) in additionalFiles.fields()) {
      if (additionalFile !is ObjectNode) continue
      additionalFile.put(NAME, path)
      taskFiles.add(additionalFile)
    }

    return stepOptionsJson
  }
}
