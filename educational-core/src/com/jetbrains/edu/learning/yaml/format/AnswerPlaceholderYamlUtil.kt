package com.jetbrains.edu.learning.yaml.format

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.intellij.openapi.util.text.StringUtil
import com.jetbrains.edu.coursecreator.yaml.formatError
import com.jetbrains.edu.coursecreator.yaml.negativeParamNotAllowedMessage
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholderDependency
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.DEPENDENCY
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.LENGTH
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.OFFSET
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.PLACEHOLDER_TEXT

@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
@JsonPropertyOrder(OFFSET, LENGTH, PLACEHOLDER_TEXT, DEPENDENCY)
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
  @JsonSerialize(using = EmptyTextSerializer::class)
  private fun getPlaceholderText(): String {
    throw NotImplementedInMixin()
  }

  @JsonProperty(DEPENDENCY)
  private var myPlaceholderDependency: AnswerPlaceholderDependency? = null
}

// TODO: This could be removed updating jackson-dataformats to 2.10. see https://github.com/FasterXML/jackson-dataformats-text/issues/50
private class EmptyTextSerializer : JsonSerializer<String>() {
  override fun serialize(placeholderText: String, generator: JsonGenerator, provider: SerializerProvider) {
    if (StringUtil.isEmpty(placeholderText)) {
      val yamlGenerator = generator as YAMLGenerator
      yamlGenerator.disable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
      yamlGenerator.writeString("")
      yamlGenerator.enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
    }
    else {
      generator.writeString(placeholderText)
    }
  }
}


@JsonPOJOBuilder(withPrefix = "")
open class AnswerPlaceholderBuilder(@JsonProperty(OFFSET) val offset: Int,
                                    @JsonProperty(LENGTH) val length: Int,
                                    @JsonProperty(PLACEHOLDER_TEXT) val placeholderText: String,
                                    @JsonProperty(DEPENDENCY) val dependency: AnswerPlaceholderDependency?) {
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
    placeholder.placeholderDependency = dependency
    return placeholder
  }
}


