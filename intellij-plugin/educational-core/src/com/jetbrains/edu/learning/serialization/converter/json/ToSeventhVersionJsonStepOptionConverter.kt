package com.jetbrains.edu.learning.serialization.converter.json

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.DEPENDENCY
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.DEPENDENCY_FILE
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.FILES
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.NAME
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.PLACEHOLDERS
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.TESTS
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.TITLE

class ToSeventhVersionJsonStepOptionConverter : JsonStepOptionsConverter {

  override fun convert(stepOptionsJson: ObjectNode): ObjectNode {
    if (stepOptionsJson.get(TITLE)?.asText() == EduNames.ADDITIONAL_MATERIALS) return stepOptionsJson

    val taskFiles = stepOptionsJson.get(FILES) ?: ObjectMapper().createArrayNode()
    val testFiles = stepOptionsJson.get(TESTS) ?: ObjectMapper().createArrayNode()

    if (taskFiles.any{ it.get(NAME).asText().endsWith(".py") }) return stepOptionsJson
    if (testFiles.any{ it.get(NAME).asText().endsWith(".py") }) return stepOptionsJson

    for (taskFile in taskFiles) {
      if (taskFile !is ObjectNode) continue
      convertTaskFile(taskFile, "src")
    }

    for (testFile in testFiles) {
      if (testFile !is ObjectNode) continue
      val path = testFile.get(NAME)?.asText() ?: continue
      testFile.put(NAME, "test/$path")
    }

    return stepOptionsJson
  }

  companion object {

    fun convertTaskFile(taskFile: ObjectNode, taskFilesRoot: String) {
      val path = taskFile.get(NAME)?.asText() ?: return
      taskFile.put(NAME, "$taskFilesRoot/$path")
      for (placeholder in taskFile.get(PLACEHOLDERS)) {
        val placeholderObject = placeholder as? ObjectNode ?: continue
        convertPlaceholder(placeholderObject, taskFilesRoot)
      }
    }

    private fun convertPlaceholder(placeholder: ObjectNode, taskFilesRoot: String) {
      val dependency = placeholder.get(DEPENDENCY) as? ObjectNode ?: return
      val dependencyFilePath = dependency.get(DEPENDENCY_FILE)?.asText() ?: return
      dependency.put(DEPENDENCY_FILE, "$taskFilesRoot/$dependencyFilePath")
    }
  }
}
