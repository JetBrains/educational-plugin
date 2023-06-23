@file:Suppress("unused")

package com.jetbrains.edu.learning.yaml.format

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.util.StdConverter
import com.jetbrains.edu.learning.courseFormat.message
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOption
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus
import com.jetbrains.edu.learning.json.mixins.FeedbackCorrectFilter
import com.jetbrains.edu.learning.json.mixins.FeedbackIncorrectFilter
import com.jetbrains.edu.learning.json.mixins.QuizHeaderFilter
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FEEDBACK_CORRECT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FEEDBACK_INCORRECT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FEEDBACK_LINK
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FILES
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.IS_CORRECT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.IS_MULTIPLE_CHOICE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.LOCAL_CHECK
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.OPTIONS
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.QUIZ_HEADER
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TAGS
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TYPE
import com.jetbrains.edu.learning.yaml.errorHandling.formatError

@Suppress("unused") // used for yaml serialization
@JsonPropertyOrder(TYPE, IS_MULTIPLE_CHOICE, OPTIONS, FEEDBACK_CORRECT, FEEDBACK_INCORRECT, QUIZ_HEADER, FILES, FEEDBACK_LINK, TAGS)
abstract class ChoiceTaskYamlMixin : TaskYamlMixin() {

  @JsonProperty(IS_MULTIPLE_CHOICE)
  private var isMultipleChoice: Boolean = false

  @JsonProperty(OPTIONS)
  private lateinit var choiceOptions: List<ChoiceOption>

  @JsonProperty(FEEDBACK_CORRECT)
  @JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = FeedbackCorrectFilter::class)
  private var messageCorrect: String = ""

  @JsonProperty(FEEDBACK_INCORRECT)
  @JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = FeedbackIncorrectFilter::class)
  private var messageIncorrect: String = ""

  @JsonProperty(QUIZ_HEADER)
  @JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = QuizHeaderFilter::class)
  private var quizHeader: String = ""

  @JsonProperty(LOCAL_CHECK)
  var canCheckLocally: Boolean = true
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
      else -> formatError(message("yaml.editor.invalid.unknown.option", value))
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