package com.jetbrains.edu.learning.yaml.format.student

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.yaml.format.FrameworkLessonYamlMixin
import com.jetbrains.edu.learning.yaml.format.LessonBuilder
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.CONTENT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.CURRENT_TASK
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.CUSTOM_NAME
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TYPE

@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
@JsonDeserialize(builder = StudentFrameworkLessonBuilder::class)
@JsonPropertyOrder(TYPE, CUSTOM_NAME, CONTENT, CURRENT_TASK)
abstract class StudentFrameworkLessonYamlMixin : FrameworkLessonYamlMixin() {
  @JsonProperty(CURRENT_TASK)
  private var currentTaskIndex: Int = 0
}

private class StudentFrameworkLessonBuilder(
  @JsonProperty(CURRENT_TASK) val currentTaskIndex: Int,
  @JsonProperty(CONTENT) content: List<String?>
) : LessonBuilder(content) {
  override fun createLesson(): Lesson {
    return FrameworkLesson().also { it.currentTaskIndex = currentTaskIndex }
  }
}