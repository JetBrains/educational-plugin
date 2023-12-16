package com.jetbrains.edu.learning.serialization.converter.json

import com.fasterxml.jackson.databind.node.ObjectNode
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtilRt
import com.jetbrains.edu.learning.serialization.SerializationUtils

class ToThirdVersionJsonStepOptionsConverter : JsonStepOptionsConverter {
  override fun convert(stepOptionsJson: ObjectNode): ObjectNode {
    if (!stepOptionsJson.has(SerializationUtils.Json.LAST_SUBTASK)) return stepOptionsJson
    val lastSubtaskIndex = stepOptionsJson[SerializationUtils.Json.LAST_SUBTASK].asInt()
    if (lastSubtaskIndex == 0) return stepOptionsJson
    val tests = stepOptionsJson[SerializationUtils.Json.TESTS]
    if (tests.size() > 0) {
      val fileWrapper = tests[0]
      if (fileWrapper.has(SerializationUtils.Json.NAME)) {
        replaceWithSubtask(fileWrapper as ObjectNode)
      }
    }
    val descriptions = stepOptionsJson[SerializationUtils.Json.TEXTS]
    if (descriptions != null && descriptions.size() > 0) {
      val fileWrapper = descriptions[0]
      if (fileWrapper.has(SerializationUtils.Json.NAME)) {
        replaceWithSubtask(fileWrapper as ObjectNode)
      }
    }
    return stepOptionsJson
  }

  companion object {
    private fun replaceWithSubtask(fileWrapper: ObjectNode) {
      val file = fileWrapper[SerializationUtils.Json.NAME].asText()
      val extension = FileUtilRt.getExtension(file)
      val name = FileUtil.getNameWithoutExtension(file)
      if (!name.contains(SerializationUtils.SUBTASK_MARKER)) {
        fileWrapper.remove(SerializationUtils.Json.NAME)
        fileWrapper.put(SerializationUtils.Json.NAME, name + "_subtask0." + extension)
      }
    }
  }
}
