package com.jetbrains.edu.coursecreator.configuration.mixins

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.util.StdConverter

@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                fieldVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder("section", "lesson", "task", "file", "placeholder", "is_visible")
abstract class AnswerPlaceholderDependencyYamlMixin {
  @JsonProperty("section")
  private var mySectionName: String? = null

  @JsonProperty("lesson")
  private lateinit var myLessonName: String

  @JsonProperty("task")
  private lateinit var myTaskName: String

  @JsonProperty("file")
  private lateinit var myFileName: String

  @JsonProperty("placeholder")
  @JsonSerialize(converter = InternalIndexToUserVisibleConverter::class)
  @JsonDeserialize(converter = UserVisibleIndexToInternalConverter::class)
  private var myPlaceholderIndex: Int = -1

  @JsonProperty("is_visible")
  private var myIsVisible = true
}

class InternalIndexToUserVisibleConverter : StdConverter<Int, Int>() {
  override fun convert(index: Int) = index + 1
}

class UserVisibleIndexToInternalConverter : StdConverter<Int, Int>() {
  override fun convert(index: Int) = index - 1
}
