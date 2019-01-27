package com.jetbrains.edu.learning.serialization.converter.json.local

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.serialization.SerializationUtils

abstract class JsonLocalCourseConverterBase : JsonLocalCourseConverter {

  override fun convert(localCourse: ObjectNode): ObjectNode {
    val language = localCourse.get(SerializationUtils.Json.PROGRAMMING_LANGUAGE)?.asText() ?: ""

    for (item in localCourse.getJsonObjectList(SerializationUtils.Json.ITEMS)) {
      val type = item.get(SerializationUtils.Json.ITEM_TYPE)?.asText()
      when (type) {
        null, EduNames.LESSON, SerializationUtils.Json.FRAMEWORK_TYPE -> convertLesson(item, language)
        EduNames.SECTION -> convertSection(item, language)
      }
    }

    return localCourse
  }

  protected fun convertSection(sectionObject: ObjectNode, language: String) {
    convertSectionObject(sectionObject, language)
    for (lesson in sectionObject.getJsonObjectList(SerializationUtils.Json.ITEMS)) {
      convertLesson(lesson, language)
    }
  }

  protected fun convertLesson(lessonObject: ObjectNode, language: String) {
    convertLessonObject(lessonObject, language)
    for (task in lessonObject.getJsonObjectList(SerializationUtils.Json.TASK_LIST)) {
      convertTaskObject(task, language)
    }
  }

  protected open fun convertSectionObject(sectionObject: ObjectNode, language: String) {}
  protected open fun convertLessonObject(lessonObject: ObjectNode, language: String) {}
  protected open fun convertTaskObject(taskObject: ObjectNode, language: String) {}

  protected fun ObjectNode.getJsonObjectList(name: String): List<ObjectNode> {
    val array = get(name) ?: return emptyList()
    return array.filterIsInstance<ObjectNode>()
  }

  protected inline fun <reified T : JsonNode> ObjectNode.getJsonObjectMap(name: String): Map<String, T> {
    val jsonObject = get(name) ?: return emptyMap()
    @Suppress("UNCHECKED_CAST")
    return jsonObject.fields().asSequence()
      .map { e -> e.key to e.value }
      .filter { it.second is T }
      .toMap() as Map<String, T>
  }
}
