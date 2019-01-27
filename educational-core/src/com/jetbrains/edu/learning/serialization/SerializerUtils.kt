package com.jetbrains.edu.learning.serialization

import com.fasterxml.jackson.core.ObjectCodec
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.tasks.*
import com.jetbrains.edu.learning.stepik.StepikNames

private val LOG = Logger.getInstance(EduUtils::class.java)

fun doDeserializeTask(node: ObjectNode, objectMapper: ObjectCodec): Task? {
  if (node.has(SerializationUtils.Json.NAME) && StepikNames.PYCHARM_ADDITIONAL == node.get(SerializationUtils.Json.NAME).asText()) {
    node.remove(SerializationUtils.Json.NAME)
    node.put(SerializationUtils.Json.NAME, EduNames.ADDITIONAL_MATERIALS)
  }
  if (node.has(SerializationUtils.Json.TASK_TYPE)) {
    val taskType = node.get(SerializationUtils.Json.TASK_TYPE).asText()
    return when (taskType) {
      "ide" -> objectMapper.treeToValue(node, IdeTask::class.java)
      "choice" -> objectMapper.treeToValue(node, ChoiceTask::class.java)
      "theory" -> objectMapper.treeToValue(node, TheoryTask::class.java)
      "code" -> objectMapper.treeToValue(node, CodeTask::class.java)
      "edu" -> objectMapper.treeToValue(node, EduTask::class.java)
      "output" -> objectMapper.treeToValue(node, OutputTask::class.java)
      "pycharm" -> objectMapper.treeToValue(node, EduTask::class.java)     // deprecated: old courses have pycharm tasks
      else -> {
        LOG.warn("Unsupported task type $taskType")
        null
      }
    }
  }
  LOG.warn("No task type found in json $node")
  return null
}
