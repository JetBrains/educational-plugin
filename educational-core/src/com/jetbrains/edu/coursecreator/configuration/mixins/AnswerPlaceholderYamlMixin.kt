package com.jetbrains.edu.coursecreator.configuration.mixins

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.intellij.openapi.util.text.StringUtil
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder


@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                fieldVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder("offset", "length", "placeholder_text", "hints")
@JsonDeserialize(builder = AnswerPlaceholderBuilder::class)
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
}

@JsonPOJOBuilder(withPrefix = "")
private class AnswerPlaceholderBuilder(@JsonProperty("offset") val offset: Int,
                                       @JsonProperty("length") val length: Int,
                                       @JsonProperty("placeholder_text") val placeholderText: String,
                                       @JsonProperty("hints") val hints: List<String>) {
  @Suppress("unused") // deserialization
  private fun build(): AnswerPlaceholder {
    val placeholder = AnswerPlaceholder()
    placeholder.length = placeholderText.length
    placeholder.placeholderText = placeholderText
    placeholder.hints = hints
    placeholder.useLength = false
    placeholder.offset = offset
    //we don't have access to real possible answer here, so we need to preserve its length to set real text later
    placeholder.possibleAnswer = StringUtil.repeatSymbol('_', length)
    return placeholder
  }
}
