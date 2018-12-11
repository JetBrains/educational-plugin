package com.jetbrains.edu.learning.serialization.converter.json.local

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.*
import com.jetbrains.edu.learning.serialization.converter.LANGUAGE_TASK_ROOTS
import com.jetbrains.edu.learning.serialization.converter.json.ToSeventhVersionJsonStepOptionConverter

class ToSeventhVersionLocalCourseConverter : JsonLocalCourseConverterBase() {

  override fun convertTaskObject(taskObject: JsonObject, language: String) {
    val taskName = taskObject.getAsJsonPrimitive(NAME)?.asString ?: return
    val taskRoots = LANGUAGE_TASK_ROOTS[language]
    if (taskRoots != null && taskName != EduNames.ADDITIONAL_MATERIALS) {
      val (taskFilesRoot, testFilesRoot) = taskRoots

      val taskFiles = JsonObject()
      for ((path, taskFileObject) in taskObject.getJsonObjectMap<JsonObject>(TASK_FILES)) {
        ToSeventhVersionJsonStepOptionConverter.convertTaskFile(taskFileObject, taskFilesRoot)
        taskFiles.add("$taskFilesRoot/$path", taskFileObject)
      }
      taskObject.add(TASK_FILES, taskFiles)

      val testFiles = JsonObject()
      for ((path, text) in taskObject.getJsonObjectMap<JsonPrimitive>(TEST_FILES)) {
        testFiles.addProperty("$testFilesRoot/$path", text.asString)
      }
      taskObject.add(TEST_FILES, testFiles)
    }

    val additionalFiles = JsonObject()
    for ((path, text) in taskObject.getJsonObjectMap<JsonPrimitive>(ADDITIONAL_FILES)) {
      val additionalFile = JsonObject()
      additionalFile.addProperty(TEXT, text.asString)
      additionalFiles.add(path, additionalFile)
    }
    taskObject.add(ADDITIONAL_FILES, additionalFiles)
  }
}
