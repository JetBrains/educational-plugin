package com.jetbrains.edu.learning.serialization.converter.json.local

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.*
import com.jetbrains.edu.learning.serialization.converter.LANGUAGE_TASK_ROOTS

class ToSeventhVersionLocalCourseConverter : JsonLocalCourseConverterBase() {

  override fun convertTaskObject(taskObject: ObjectNode, language: String) {
    val taskName = taskObject.get(NAME)?.asText() ?: return
    val taskRoots = LANGUAGE_TASK_ROOTS[language]
    if (taskRoots != null && taskName != EduNames.ADDITIONAL_MATERIALS) {
      val (taskFilesRoot, testFilesRoot) = taskRoots

      val taskFiles = ObjectMapper().createObjectNode()
      for ((path, taskFileObject) in taskObject.getJsonObjectMap<ObjectNode>(TASK_FILES)) {
        convertTaskFile(taskFileObject, taskFilesRoot)
        taskFiles.set("$taskFilesRoot/$path", taskFileObject)
      }
      taskObject.set(TASK_FILES, taskFiles)

      val testFiles = ObjectMapper().createObjectNode()
      for ((path, text) in taskObject.getJsonObjectMap<JsonNode>(TEST_FILES)) {
        testFiles.put("$testFilesRoot/$path", text.asText())
      }
      taskObject.set(TEST_FILES, testFiles)
    }

    val additionalFiles = ObjectMapper().createObjectNode()
    for ((path, text) in taskObject.getJsonObjectMap<JsonNode>(ADDITIONAL_FILES)) {
      val additionalFile = ObjectMapper().createObjectNode()
      additionalFile.put(TEXT, text.asText())
      additionalFiles.set(path, additionalFile)
    }
    taskObject.set(ADDITIONAL_FILES, additionalFiles)
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
