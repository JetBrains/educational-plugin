package com.jetbrains.edu.learning.yaml.format

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholderDependency
import com.jetbrains.edu.learning.yaml.errorHandling.formatError
import com.jetbrains.edu.learning.yaml.errorHandling.negativeLengthNotAllowedMessage
import com.jetbrains.edu.learning.yaml.errorHandling.negativeOffsetNotAllowedMessage
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.DEPENDENCY
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.LENGTH
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.OFFSET
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.PLACEHOLDER_TEXT

@Suppress("unused") // used for yaml serialization
@JsonPropertyOrder(OFFSET, LENGTH, PLACEHOLDER_TEXT, DEPENDENCY)
@JsonDeserialize(builder = AnswerPlaceholderBuilder::class)
abstract class AnswerPlaceholderYamlMixin {
  @JsonProperty(OFFSET)
  private var offset: Int = -1

  @JsonProperty(LENGTH)
  private var length: Int = -1

  @JsonProperty(PLACEHOLDER_TEXT)
  @JsonInclude(JsonInclude.Include.ALWAYS)
  private lateinit var placeholderText: String

  @JsonProperty(DEPENDENCY)
  private var placeholderDependency: AnswerPlaceholderDependency? = null
}

@JsonPOJOBuilder(withPrefix = "")
open class AnswerPlaceholderBuilder(
  @JsonProperty(OFFSET) val offset: Int,
  @JsonProperty(LENGTH) val length: Int,
  @JsonProperty(PLACEHOLDER_TEXT) val placeholderText: String,
  @JsonProperty(DEPENDENCY) val dependency: AnswerPlaceholderDependency?
) {
  @Suppress("unused") // deserialization
  private fun build(): AnswerPlaceholder {
    return createPlaceholder()
  }

  protected open fun createPlaceholder(): AnswerPlaceholder {
    val placeholder = AnswerPlaceholder()
    if (length < 0) {
      formatError(negativeLengthNotAllowedMessage())
    }

    if (offset < 0) {
      formatError(negativeOffsetNotAllowedMessage())
    }
    placeholder.length = length
    placeholder.placeholderText = placeholderText
    placeholder.offset = offset
    placeholder.placeholderDependency = dependency
    placeholder.init()
    return placeholder
  }
}
