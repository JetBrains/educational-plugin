package com.jetbrains.edu.learning.yaml.format

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholderDependency
import com.jetbrains.edu.learning.json.mixins.TrueValueFilter
import com.jetbrains.edu.learning.json.mixins.takeIsVisibleFromDependency
import com.jetbrains.edu.learning.yaml.errorHandling.formatError
import com.jetbrains.edu.learning.yaml.errorHandling.negativeLengthNotAllowedMessage
import com.jetbrains.edu.learning.yaml.errorHandling.negativeOffsetNotAllowedMessage
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.DEPENDENCY
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.IS_VISIBLE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.LENGTH
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.OFFSET
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.PLACEHOLDER_TEXT

@Suppress("unused") // used for yaml serialization
@JsonPropertyOrder(OFFSET, LENGTH, PLACEHOLDER_TEXT, DEPENDENCY, IS_VISIBLE)
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

  @JsonProperty(IS_VISIBLE)
  @JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = TrueValueFilter::class)
  private var isVisible: Boolean = true
}

@JsonPOJOBuilder(withPrefix = "")
open class AnswerPlaceholderBuilder(
  @param:JsonProperty(OFFSET) val offset: Int,
  @param:JsonProperty(LENGTH) val length: Int,
  @param:JsonProperty(PLACEHOLDER_TEXT) val placeholderText: String,
  @param:JsonProperty(DEPENDENCY) val dependency: AnswerPlaceholderDependency?,
  @param:JsonProperty(IS_VISIBLE) val isVisible: Boolean = true
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
    placeholder.isVisible = isVisible
    placeholder.takeIsVisibleFromDependency()
    placeholder.init()
    return placeholder
  }
}
