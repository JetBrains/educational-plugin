package com.jetbrains.edu.coursecreator.yaml.format

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholderDependency

private const val OFFSET = "offset"
private const val LENGTH = "length"
private const val PLACEHOLDER_TEXT = "placeholder_text"
private const val HINTS = "hints"
private const val DEPENDENCY = "dependency"

@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
@JsonPropertyOrder(OFFSET, LENGTH, PLACEHOLDER_TEXT, HINTS, DEPENDENCY)
@JsonDeserialize(builder = AnswerPlaceholderBuilder::class)
abstract class AnswerPlaceholderYamlMixin {
  @JsonProperty(OFFSET)
  private var myOffset: Int? = -1

  @JsonProperty(LENGTH)
  private fun getLength(): Int {
    throw NotImplementedInMixin()
  }

  @JsonProperty(PLACEHOLDER_TEXT)
  private fun getPlaceholderText(): String {
    throw NotImplementedInMixin()
  }

  @JsonProperty(HINTS)
  fun getHints(): List<String> {
    throw NotImplementedInMixin()
  }

  @JsonProperty(DEPENDENCY)
  private var myPlaceholderDependency: AnswerPlaceholderDependency? = null
}

@JsonPOJOBuilder(withPrefix = "")
private class AnswerPlaceholderBuilder(@JsonProperty(OFFSET) val offset: Int,
                                       @JsonProperty(LENGTH) val length: Int,
                                       @JsonProperty(PLACEHOLDER_TEXT) val placeholderText: String,
                                       @JsonProperty(HINTS) val hints: List<String> = mutableListOf(),
                                       @JsonProperty(DEPENDENCY) val dependency: AnswerPlaceholderDependency?) {
  @Suppress("unused") // deserialization
  private fun build(): AnswerPlaceholder {
    val placeholder = AnswerPlaceholder()
    placeholder.length = length
    placeholder.placeholderText = placeholderText
    placeholder.hints = hints
    placeholder.offset = offset
    placeholder.placeholderDependency = dependency
    return placeholder
  }
}
