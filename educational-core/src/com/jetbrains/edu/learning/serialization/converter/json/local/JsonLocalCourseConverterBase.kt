package com.jetbrains.edu.learning.serialization.converter.json.local

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.serialization.SerializationUtils

abstract class JsonLocalCourseConverterBase : JsonLocalCourseConverter {

  override fun convert(localCourse: JsonObject): JsonObject {
    val language = localCourse.getAsJsonPrimitive(SerializationUtils.Json.PROGRAMMING_LANGUAGE)?.asString ?: ""

    for (item in localCourse.getJsonObjectList(SerializationUtils.Json.ITEMS)) {
      val type = item.getAsJsonPrimitive(SerializationUtils.Json.ITEM_TYPE)?.asString
      when (type) {
        null, EduNames.LESSON, SerializationUtils.Json.FRAMEWORK_TYPE -> convertLesson(item, language)
        EduNames.SECTION -> convertSection(item, language)
      }
    }

    return localCourse
  }

  protected fun convertSection(sectionObject: JsonObject, language: String) {
    convertSectionObject(sectionObject, language)
    for (lesson in sectionObject.getJsonObjectList(SerializationUtils.Json.ITEMS)) {
      convertLesson(lesson, language)
    }
  }

  protected fun convertLesson(lessonObject: JsonObject, language: String) {
    convertLessonObject(lessonObject, language)
    for (task in lessonObject.getJsonObjectList(SerializationUtils.Json.TASK_LIST)) {
      convertTaskObject(task, language)
    }
  }

  protected open fun convertSectionObject(sectionObject: JsonObject, language: String) {}
  protected open fun convertLessonObject(lessonObject: JsonObject, language: String) {}
  protected open fun convertTaskObject(taskObject: JsonObject, language: String) {}

  protected fun JsonObject.getJsonObjectList(name: String): List<JsonObject> {
    val array = getAsJsonArray(name) ?: return emptyList()
    return array.filterIsInstance<JsonObject>()
  }

  protected inline fun <reified T : JsonElement> JsonObject.getJsonObjectMap(name: String): Map<String, T> {
    val jsonObject = getAsJsonObject(name) ?: return emptyMap()
    @Suppress("UNCHECKED_CAST")
    return jsonObject.entrySet().asSequence()
      .map { e -> e.key to e.value }
      .filter { it.second is T }
      .toMap() as Map<String, T>
  }
}
