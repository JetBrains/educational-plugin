package com.jetbrains.edu.learning.yaml.format

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.yaml.errorHandling.formatError
import com.jetbrains.edu.learning.yaml.errorHandling.negativeParamNotAllowedMessage
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.LENGTH
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.OFFSET
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.PLACEHOLDER_TEXT

@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
@JsonPropertyOrder(OFFSET, LENGTH, PLACEHOLDER_TEXT)
@JsonDeserialize(builder = AnswerPlaceholderBuilder::class)
abstract class AnswerPlaceholderYamlMixin {
  @JsonProperty(OFFSET)
  private var myOffset: Int? = -1

  @JsonProperty(LENGTH)
  private fun getLength(): Int {
    throw NotImplementedInMixin()
  }

  @JsonProperty(PLACEHOLDER_TEXT)
  @JsonInclude(JsonInclude.Include.ALWAYS)
  private fun getPlaceholderText(): String {
    throw NotImplementedInMixin()
  }

}

@JsonPOJOBuilder(withPrefix = "")
open class AnswerPlaceholderBuilder(@JsonProperty(OFFSET) val offset: Int,
                                    @JsonProperty(LENGTH) val length: Int,
                                    @JsonProperty(PLACEHOLDER_TEXT) val placeholderText: String) {
  @Suppress("unused") // deserialization
  private fun build(): AnswerPlaceholder {
    return createPlaceholder()
  }

  protected open fun createPlaceholder(): AnswerPlaceholder {
    val placeholder = AnswerPlaceholder()
    if (length < 0) {
      formatError(negativeParamNotAllowedMessage(LENGTH))
    }

    if (offset < 0) {
      formatError(negativeParamNotAllowedMessage(OFFSET))
    }
    placeholder.length = length
    placeholder.placeholderText = placeholderText
    placeholder.offset = offset
    return placeholder
  }
}


