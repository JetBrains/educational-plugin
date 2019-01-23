package com.jetbrains.edu.learning.serialization.converter.json

import com.fasterxml.jackson.databind.node.ObjectNode
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.*
import com.jetbrains.edu.learning.serialization.converter.LANGUAGE_TASK_ROOTS

class ToSeventhVersionJsonStepOptionConverter(private val language: String?) : JsonStepOptionsConverter {

  override fun convert(stepOptionsJson: ObjectNode): ObjectNode {
    if (language == null) return stepOptionsJson
    if (stepOptionsJson.get(TITLE)?.asText() == EduNames.ADDITIONAL_MATERIALS) return stepOptionsJson
    val (taskFilesRoot, testFilesRoot) = LANGUAGE_TASK_ROOTS[language] ?: return stepOptionsJson

    val taskFiles = stepOptionsJson.get(FILES)
    if (taskFiles != null) {
      for (taskFile in taskFiles) {
        if (taskFile !is ObjectNode) continue
        convertTaskFile(taskFile, taskFilesRoot)
      }
    }

    val testFiles = stepOptionsJson.get(TESTS)
    if (testFiles != null) {
      for (testFile in testFiles) {
        if (testFile !is ObjectNode) continue
        val path = testFile.get(NAME)?.asText() ?: continue
        testFile.put(NAME, "$testFilesRoot/$path")
      }
    }

    return stepOptionsJson
  }

  companion object {

    @JvmStatic
    fun convertTaskFile(taskFile: ObjectNode, taskFilesRoot: String) {
      val path = taskFile.get(NAME)?.asText() ?: return
      taskFile.put(NAME, "$taskFilesRoot/$path")
      for (placeholder in taskFile.get(PLACEHOLDERS)) {
        val placeholderObject = placeholder as? ObjectNode ?: continue
        convertPlaceholder(placeholderObject, taskFilesRoot)
      }
    }

    @JvmStatic
    fun convertPlaceholder(placeholder: ObjectNode, taskFilesRoot: String) {
      val dependency = placeholder.get(DEPENDENCY) as? ObjectNode ?: return
      val dependencyFilePath = dependency.get(DEPENDENCY_FILE)?.asText() ?: return
      dependency.put(DEPENDENCY_FILE, "$taskFilesRoot/$dependencyFilePath")
    }
  }
}
