package com.jetbrains.edu.learning.serialization.converter.json

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.*
import com.jetbrains.edu.learning.serialization.converter.LANGUAGE_TASK_ROOTS

class ToSeventhVersionLocalCourseConverter : JsonLocalCourseConverter {

  override fun convert(localCourse: JsonObject): JsonObject {
    val language = localCourse.getAsJsonPrimitive(PROGRAMMING_LANGUAGE)?.asString ?: ""

    for (item in localCourse.getJsonObjectList(ITEMS)) {
      val type = item.getAsJsonPrimitive(ITEM_TYPE)?.asString
      when (type) {
        null, EduNames.LESSON, FRAMEWORK_TYPE -> convertLesson(item, language)
        EduNames.SECTION -> convertSection(item, language)
      }
    }

    return localCourse
  }

  private fun convertSection(sectionObject: JsonObject, language: String) {
    for (lesson in sectionObject.getJsonObjectList(ITEMS)) {
      convertLesson(lesson, language)
    }
  }

  private fun convertLesson(lessonObject: JsonObject, language: String) {
    for (task in lessonObject.getJsonObjectList(TASK_LIST)) {
      convertTask(task, language)
    }
  }
  
  private fun convertTask(taskObject: JsonObject, language: String) {
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

  private fun JsonObject.getJsonObjectList(name: String): List<JsonObject> {
    val array = getAsJsonArray(name) ?: return emptyList()
    return array.filterIsInstance<JsonObject>()
  }
  
  private inline fun <reified T : JsonElement> JsonObject.getJsonObjectMap(name: String): Map<String, T> {
    val jsonObject = getAsJsonObject(name) ?: return emptyMap()
    @Suppress("UNCHECKED_CAST")
    return jsonObject.entrySet().asSequence()
      .map { e -> e.key to e.value }
      .filter { it.second is T }
      .toMap() as Map<String, T>
  }
}
