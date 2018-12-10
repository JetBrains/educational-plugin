package com.jetbrains.edu.coursecreator.configuration.mixins

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task

private const val TYPE = "type"
private const val CONTENT = "content"

@JsonDeserialize(builder = FrameworkLessonBuilder::class)
@JsonPropertyOrder(TYPE, CONTENT)
abstract class FrameworkLessonYamlUtil : LessonYamlMixin() {
  @JsonProperty(CONTENT)
  @JsonSerialize(contentConverter = StudyItemConverter::class)
  private lateinit var taskList: List<Task>

  @JsonProperty(TYPE)
  fun getType(): String {
    throw NotImplementedError()
  }
}

@JsonPOJOBuilder(withPrefix = "")
private class FrameworkLessonBuilder(@JsonProperty(CONTENT) val content: List<String?>) {
  @Suppress("unused") //used for deserialization
  private fun build(): FrameworkLesson {
    val lesson = FrameworkLesson()
    val items = parseTaskList(content)
    lesson.updateTaskList(items)
    return lesson
  }
}
