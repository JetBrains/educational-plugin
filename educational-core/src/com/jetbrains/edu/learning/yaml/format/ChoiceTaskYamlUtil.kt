@file:Suppress("unused")

package com.jetbrains.edu.learning.yaml.format

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.util.StdConverter
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOption
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FEEDBACK_CORRECT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FEEDBACK_INCORRECT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FEEDBACK_LINK
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FILES
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.IS_CORRECT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.IS_MULTIPLE_CHOICE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.OPTIONS
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TYPE
import com.jetbrains.edu.learning.yaml.format.errorHandling.formatError


@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
@JsonPropertyOrder(TYPE, IS_MULTIPLE_CHOICE, OPTIONS, FEEDBACK_CORRECT, FEEDBACK_INCORRECT, FILES, FEEDBACK_LINK)
abstract class ChoiceTaskYamlMixin : TaskYamlMixin() {

  @JsonProperty(IS_MULTIPLE_CHOICE)
  private var isMultipleChoice: Boolean = false

  @JsonProperty(OPTIONS)
  private lateinit var choiceOptions: List<ChoiceOption>

  @JsonProperty(FEEDBACK_CORRECT)
  private var messageCorrect: String = ""

  @JsonProperty(FEEDBACK_INCORRECT)
  private var messageIncorrect: String = ""
}

abstract class ChoiceOptionYamlMixin {
  @JsonProperty
  private var text: String = ""

  @JsonProperty(IS_CORRECT)
  @JsonSerialize(converter = FromChoiceOptionStatusConverter::class)
  @JsonDeserialize(converter = ToChoiceOptionStatusConverter::class)
  @JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = UnknownOptionFilter::class)
  private var status: ChoiceOptionStatus = ChoiceOptionStatus.UNKNOWN
}

private class FromChoiceOptionStatusConverter : StdConverter<ChoiceOptionStatus, Boolean>() {
  override fun convert(value: ChoiceOptionStatus): Boolean {
    return when (value) {
      ChoiceOptionStatus.CORRECT -> true
      ChoiceOptionStatus.INCORRECT -> false
      else -> formatError("Unknown option status not allowed")
    }
  }
}

private class ToChoiceOptionStatusConverter : StdConverter<Boolean?, ChoiceOptionStatus>() {
  override fun convert(value: Boolean?): ChoiceOptionStatus {
    if (value == null) {
      return ChoiceOptionStatus.UNKNOWN
    }
    return if (value) ChoiceOptionStatus.CORRECT else ChoiceOptionStatus.INCORRECT
  }
}

@Suppress("EqualsOrHashCode")
private class UnknownOptionFilter {
  override fun equals(other: Any?): Boolean {
    if (other == null || other !is ChoiceOptionStatus) {
      return false
    }
    return other == ChoiceOptionStatus.UNKNOWN
  }
}