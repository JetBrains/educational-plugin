package com.jetbrains.edu.learning.yaml.format

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.util.StdConverter
import com.jetbrains.edu.learning.json.mixins.TrueValueFilter
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FILE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.IS_VISIBLE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.LESSON
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.PLACEHOLDER
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.SECTION
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TASK

@Suppress("unused") // used for yaml serialization
@JsonPropertyOrder(SECTION, LESSON, TASK, FILE, PLACEHOLDER, IS_VISIBLE)
abstract class AnswerPlaceholderDependencyYamlMixin {
  @JsonProperty(SECTION)
  private var sectionName: String? = null

  @JsonProperty(LESSON)
  private lateinit var lessonName: String

  @JsonProperty(TASK)
  private lateinit var taskName: String

  @JsonProperty(FILE)
  private lateinit var fileName: String

  @JsonProperty(PLACEHOLDER)
  @JsonSerialize(converter = InternalIndexToUserVisibleConverter::class)
  @JsonDeserialize(converter = UserVisibleIndexToInternalConverter::class)
  private var placeholderIndex: Int = -1

  @JsonProperty(IS_VISIBLE)
  @JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = TrueValueFilter::class)
  private var isVisible = true
}

private class InternalIndexToUserVisibleConverter : StdConverter<Int, Int>() {
  override fun convert(index: Int) = index + 1
}

private class UserVisibleIndexToInternalConverter : StdConverter<Int, Int>() {
  override fun convert(index: Int) = index - 1
}
