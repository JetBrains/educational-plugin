package com.jetbrains.edu.learning.serialization.converter.json

import com.fasterxml.jackson.databind.node.ObjectNode
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.serialization.SerializationUtils
import com.jetbrains.edu.learning.stepik.StepikNames

class ToFourthVersionJsonStepOptionsConverter : JsonStepOptionsConverter {
  override fun convert(stepOptionsJson: ObjectNode): ObjectNode {
    if (stepOptionsJson.has(SerializationUtils.Json.TITLE)
        && StepikNames.PYCHARM_ADDITIONAL == stepOptionsJson[SerializationUtils.Json.TITLE].asText()) {
      stepOptionsJson.remove(SerializationUtils.Json.TITLE)
      stepOptionsJson.put(SerializationUtils.Json.TITLE, EduNames.ADDITIONAL_MATERIALS)
    }
    return stepOptionsJson
  }
}
