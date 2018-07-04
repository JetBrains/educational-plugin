package com.jetbrains.edu.coursecreator.configuration.mixins

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholderDependency

@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                fieldVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder("offset", "length", "placeholder_text", "hints", "dependency")
abstract class AnswerPlaceholderYamlMixin {
  @JsonProperty("offset")
  private var myOffset: Int? = -1

  @JsonProperty("length")
  private fun getRealLength(): Int {
    throw NotImplementedInMixin()
  }

  @JsonProperty("placeholder_text")
  private fun getPlaceholderText(): String {
    throw NotImplementedInMixin()
  }

  @JsonProperty("hints")
  fun getHints(): List<String> {
    throw NotImplementedInMixin()
  }

  @JsonProperty("dependency")
  private var myPlaceholderDependency: AnswerPlaceholderDependency? = null

}
