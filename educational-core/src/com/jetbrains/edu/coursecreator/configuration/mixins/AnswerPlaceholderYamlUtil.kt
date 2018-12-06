package com.jetbrains.edu.coursecreator.configuration.mixins

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.intellij.openapi.util.text.StringUtil
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholderDependency

private const val OFFSET = "offset"
private const val LENGTH = "length"
private const val PLACEHOLDER_TEXT = "placeholder_text"
private const val HINTS = "hints"
private const val DEPENDENCY = "dependency"

@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                fieldVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder(OFFSET, LENGTH, PLACEHOLDER_TEXT, HINTS, DEPENDENCY)
@JsonDeserialize(builder = AnswerPlaceholderBuilder::class)
abstract class AnswerPlaceholderYamlMixin {
  @JsonProperty(OFFSET)
  private var myOffset: Int? = -1

  @JsonProperty(LENGTH)
  private fun getRealLength(): Int {
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
    placeholder.length = placeholderText.length
    placeholder.placeholderText = placeholderText
    placeholder.hints = hints
    placeholder.useLength = false
    placeholder.offset = offset
    placeholder.placeholderDependency = dependency
    //we don't have access to real possible answer here, so we need to preserve its length to set real text later
    placeholder.possibleAnswer = StringUtil.repeatSymbol('_', length)
    return placeholder
  }
}
