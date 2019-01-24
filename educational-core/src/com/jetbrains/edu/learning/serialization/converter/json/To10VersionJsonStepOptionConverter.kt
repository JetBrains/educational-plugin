package com.jetbrains.edu.learning.serialization.converter.json

import com.fasterxml.jackson.databind.node.ObjectNode
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.DESCRIPTION_FORMAT

class To10VersionJsonStepOptionConverter : JsonStepOptionsConverter {
  override fun convert(stepOptionsJson: ObjectNode): ObjectNode {
    val descriptionFormat = stepOptionsJson.get(DESCRIPTION_FORMAT).asText()
    stepOptionsJson.put(DESCRIPTION_FORMAT, descriptionFormat.toUpperCase())
    return stepOptionsJson
  }
}
