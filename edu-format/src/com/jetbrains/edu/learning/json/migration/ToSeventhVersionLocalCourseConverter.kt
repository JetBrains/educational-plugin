package com.jetbrains.edu.learning.json.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.NAME
import com.jetbrains.edu.learning.json.migration.MigrationNames.ADDITIONAL_MATERIALS
import com.jetbrains.edu.learning.json.migration.MigrationNames.DEPENDENCY_FILE
import com.jetbrains.edu.learning.json.migration.MigrationNames.TASK_FILES
import com.jetbrains.edu.learning.json.migration.MigrationNames.TEST_FILES
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.ADDITIONAL_FILES
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.DEPENDENCY
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.PLACEHOLDERS
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.TEXT

class ToSeventhVersionLocalCourseConverter : JsonLocalCourseConverterBase() {

  override fun convertTaskObject(taskObject: ObjectNode, language: String) {
    val taskName = taskObject.get(NAME)?.asText() ?: return
    val taskRoots = LANGUAGE_TASK_ROOTS[language]
    if (taskRoots != null && taskName != ADDITIONAL_MATERIALS) {
      val (taskFilesRoot, testFilesRoot) = taskRoots

      val taskFiles = ObjectMapper().createObjectNode()
      for ((path, taskFileObject) in taskObject.getJsonObjectMap<ObjectNode>(TASK_FILES)) {
        convertTaskFile(taskFileObject, taskFilesRoot)
        taskFiles.set<JsonNode?>("$taskFilesRoot/$path", taskFileObject)
      }
      taskObject.set<JsonNode?>(TASK_FILES, taskFiles)

      val testFiles = ObjectMapper().createObjectNode()
      for ((path, text) in taskObject.getJsonObjectMap<JsonNode>(TEST_FILES)) {
        testFiles.put("$testFilesRoot/$path", text.asText())
      }
      taskObject.set<JsonNode?>(TEST_FILES, testFiles)
    }

    val additionalFiles = ObjectMapper().createObjectNode()
    for ((path, text) in taskObject.getJsonObjectMap<JsonNode>(ADDITIONAL_FILES)) {
      val additionalFile = ObjectMapper().createObjectNode()
      additionalFile.put(TEXT, text.asText())
      additionalFiles.set<JsonNode?>(path, additionalFile)
    }
    taskObject.set<JsonNode?>(ADDITIONAL_FILES, additionalFiles)
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
