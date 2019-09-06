package com.jetbrains.edu.learning.yaml.format.student

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.yaml.format.ChoiceTaskYamlMixin
import com.jetbrains.edu.learning.yaml.format.ChoiceTaskYamlMixin.Companion.FEEDBACK_CORRECT
import com.jetbrains.edu.learning.yaml.format.ChoiceTaskYamlMixin.Companion.FEEDBACK_INCORRECT
import com.jetbrains.edu.learning.yaml.format.ChoiceTaskYamlMixin.Companion.IS_MULTIPLE_CHOICE
import com.jetbrains.edu.learning.yaml.format.ChoiceTaskYamlMixin.Companion.OPTIONS
import com.jetbrains.edu.learning.yaml.format.TaskYamlMixin
import com.jetbrains.edu.learning.yaml.format.TaskYamlMixin.Companion.FEEDBACK_LINK
import com.jetbrains.edu.learning.yaml.format.TaskYamlMixin.Companion.FILES

private const val SELECTED_OPTIONS = "selected_options"
private const val STATUS = "status"
private const val RECORD = "record"

@JsonPropertyOrder(TaskYamlMixin.TYPE, IS_MULTIPLE_CHOICE, OPTIONS, FEEDBACK_CORRECT, FEEDBACK_INCORRECT, FILES, FEEDBACK_LINK,
                   OPTIONS, STATUS, RECORD)
@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
abstract class StudentChoiceTaskYamlMixin: ChoiceTaskYamlMixin() {
  @JsonProperty(SELECTED_OPTIONS)
  private var selectedVariants = mutableListOf<Int>()

  @JsonProperty(STATUS)
  private lateinit var myStatus: CheckStatus

  @JsonProperty(RECORD)
  private var myRecord: Int = -1
}