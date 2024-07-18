package com.jetbrains.edu.learning.json.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.FRAMEWORK_TYPE
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.ITEMS
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.ITEM_TYPE
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.LESSON
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.PROGRAMMING_LANGUAGE
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.SECTION
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.TASK_LIST

abstract class JsonLocalCourseConverterBase : JsonLocalCourseConverter {

  override fun convert(localCourse: ObjectNode): ObjectNode {
    val language = localCourse.get(PROGRAMMING_LANGUAGE)?.asText() ?: ""

    for (item in localCourse.getJsonObjectList(ITEMS)) {
      val type = item.get(ITEM_TYPE)?.asText()
      when (type) {
        null, LESSON, FRAMEWORK_TYPE -> convertLesson(item, language)
        SECTION -> convertSection(item, language)
      }
    }

    return localCourse
  }

  private fun convertSection(sectionObject: ObjectNode, language: String) {
    convertSectionObject(sectionObject, language)
    for (lesson in sectionObject.getJsonObjectList(ITEMS)) {
      convertLesson(lesson, language)
    }
  }

  private fun convertLesson(lessonObject: ObjectNode, language: String) {
    convertLessonObject(lessonObject, language)
    for (task in lessonObject.getJsonObjectList(TASK_LIST)) {
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
