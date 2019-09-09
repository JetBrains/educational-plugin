package com.jetbrains.edu.learning.yaml.format

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.coursecreator.yaml.format.YamlMixinNames.TYPE
import com.jetbrains.edu.coursecreator.yaml.format.YamlMixinNames.CONTENT
import com.jetbrains.edu.coursecreator.yaml.format.YamlMixinNames.CUSTOM_NAME

@JsonDeserialize(builder = FrameworkLessonBuilder::class)
@JsonPropertyOrder(TYPE, CUSTOM_NAME, CONTENT)
abstract class FrameworkLessonYamlMixin : LessonYamlMixin() {
  @JsonProperty(TYPE)
  private fun getItemType(): String {
    throw NotImplementedInMixin()
  }
}

@JsonPOJOBuilder(withPrefix = "")
private class FrameworkLessonBuilder(@JsonProperty(CONTENT) content: List<String?>) : LessonBuilder(content) {
  override fun createLesson() = FrameworkLesson()
}
