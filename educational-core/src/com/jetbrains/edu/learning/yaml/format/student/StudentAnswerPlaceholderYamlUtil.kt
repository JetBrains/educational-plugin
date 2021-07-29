package com.jetbrains.edu.learning.yaml.format.student

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.encrypt.Encrypt
import com.jetbrains.edu.learning.yaml.format.AnswerPlaceholderBuilder
import com.jetbrains.edu.learning.yaml.format.AnswerPlaceholderYamlMixin
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.ENCRYPTED_POSSIBLE_ANSWER
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.INITIAL_STATE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.LENGTH
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.OFFSET
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.PLACEHOLDER_TEXT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.POSSIBLE_ANSWER
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.SELECTED
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.STATUS
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.STUDENT_ANSWER

@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
@JsonDeserialize(builder = EduAnswerPlaceholderBuilder::class)
@JsonPropertyOrder(OFFSET, LENGTH, PLACEHOLDER_TEXT, INITIAL_STATE, POSSIBLE_ANSWER, SELECTED, STATUS,
                   STUDENT_ANSWER, LENGTH, OFFSET)
abstract class StudentAnswerPlaceholderYamlMixin : AnswerPlaceholderYamlMixin() {

  @JsonProperty(INITIAL_STATE)
  private var myInitialState: AnswerPlaceholder.MyInitialState? = null

  @JsonProperty(ENCRYPTED_POSSIBLE_ANSWER)
  @Encrypt
  private var myPossibleAnswer = ""

  @JsonProperty(SELECTED)
  private var mySelected = false

  @JsonProperty(STATUS)
  private var myStatus = CheckStatus.Unchecked

  @JsonProperty(STUDENT_ANSWER)
  private var myStudentAnswer: String? = null
}

class InitialStateMixin {
  @JsonProperty(LENGTH)
  private var length = -1

  @JsonProperty(OFFSET)
  private var offset = -1
}

class EduAnswerPlaceholderBuilder(
  private val initialState: AnswerPlaceholder.MyInitialState,
  private val possibleAnswer: String = "",
  @Encrypt @JsonProperty(ENCRYPTED_POSSIBLE_ANSWER) private val encryptedPossibleAnswer: String?,
  private val selected: Boolean,
  private val status: CheckStatus,
  private val studentAnswer: String? = null,
  offset: Int,
  length: Int,
  placeholderText: String
) : AnswerPlaceholderBuilder(offset, length, placeholderText) {
  override fun createPlaceholder(): AnswerPlaceholder {
    val placeholder = super.createPlaceholder()
    placeholder.initialState = initialState
    placeholder.possibleAnswer = encryptedPossibleAnswer ?: possibleAnswer
    placeholder.selected = selected
    placeholder.status = status
    placeholder.studentAnswer = studentAnswer
    return placeholder
  }
}
