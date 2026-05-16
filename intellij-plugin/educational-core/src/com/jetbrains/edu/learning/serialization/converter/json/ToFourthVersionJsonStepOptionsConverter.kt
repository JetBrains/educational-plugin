package com.jetbrains.edu.learning.serialization.converter.json

import com.fasterxml.jackson.databind.node.ObjectNode
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.serialization.SerializationUtils

class ToFourthVersionJsonStepOptionsConverter : JsonStepOptionsConverter {
  override fun convert(stepOptionsJson: ObjectNode): ObjectNode {
    if (stepOptionsJson.has(SerializationUtils.Json.TITLE)
        && "PyCharm additional materials" == stepOptionsJson[SerializationUtils.Json.TITLE].asText()) {
      stepOptionsJson.remove(SerializationUtils.Json.TITLE)
      stepOptionsJson.put(SerializationUtils.Json.TITLE, EduNames.ADDITIONAL_MATERIALS)
    }
    return stepOptionsJson
  }
}
