package com.jetbrains.edu.learning.yaml.format

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.util.StdConverter
import com.jetbrains.edu.coursecreator.yaml.format.YamlMixinNames.FILE
import com.jetbrains.edu.coursecreator.yaml.format.YamlMixinNames.IS_VISIBLE
import com.jetbrains.edu.coursecreator.yaml.format.YamlMixinNames.LESSON
import com.jetbrains.edu.coursecreator.yaml.format.YamlMixinNames.PLACEHOLDER
import com.jetbrains.edu.coursecreator.yaml.format.YamlMixinNames.SECTION
import com.jetbrains.edu.coursecreator.yaml.format.YamlMixinNames.TASK

@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
@JsonPropertyOrder(SECTION, LESSON, TASK, FILE, PLACEHOLDER, IS_VISIBLE)
abstract class AnswerPlaceholderDependencyYamlMixin {
  @JsonProperty(SECTION)
  private var mySectionName: String? = null

  @JsonProperty(LESSON)
  private lateinit var myLessonName: String

  @JsonProperty(TASK)
  private lateinit var myTaskName: String

  @JsonProperty(FILE)
  private lateinit var myFileName: String

  @JsonProperty(PLACEHOLDER)
  @JsonSerialize(converter = InternalIndexToUserVisibleConverter::class)
  @JsonDeserialize(converter = UserVisibleIndexToInternalConverter::class)
  private var myPlaceholderIndex: Int = -1

  @JsonProperty(IS_VISIBLE)
  private var myIsVisible = true
}

private class InternalIndexToUserVisibleConverter : StdConverter<Int, Int>() {
  override fun convert(index: Int) = index + 1
}

private class UserVisibleIndexToInternalConverter : StdConverter<Int, Int>() {
  override fun convert(index: Int) = index - 1
}
