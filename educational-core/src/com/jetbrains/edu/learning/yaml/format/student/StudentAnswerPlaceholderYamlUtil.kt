package com.jetbrains.edu.learning.yaml.format.student

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholderDependency
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.yaml.format.AnswerPlaceholderBuilder
import com.jetbrains.edu.learning.yaml.format.AnswerPlaceholderYamlMixin
import com.jetbrains.edu.learning.yaml.format.AnswerPlaceholderYamlMixin.Companion.DEPENDENCY
import com.jetbrains.edu.learning.yaml.format.AnswerPlaceholderYamlMixin.Companion.LENGTH
import com.jetbrains.edu.learning.yaml.format.AnswerPlaceholderYamlMixin.Companion.OFFSET
import com.jetbrains.edu.learning.yaml.format.AnswerPlaceholderYamlMixin.Companion.PLACEHOLDER_TEXT

private const val INITIAL_STATE = "initial_state"
private const val INIT_FROM_DEPENDENCY = "initialized_from_dependency"
private const val POSSIBLE_ANSWER = "possible_answer"
private const val SELECTED = "selected"
private const val STATUS = "status"
private const val STUDENT_ANSWER = "student_answer"

@JsonDeserialize(builder = EduAnswerPlaceholderBuilder::class)
@JsonPropertyOrder(OFFSET, LENGTH, PLACEHOLDER_TEXT, DEPENDENCY,
                   INITIAL_STATE, INIT_FROM_DEPENDENCY, POSSIBLE_ANSWER, SELECTED, STATUS, STUDENT_ANSWER, LENGTH, OFFSET)
@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
class StudentAnswerPlaceholderYamlMixin : AnswerPlaceholderYamlMixin() {

  @JsonProperty(INITIAL_STATE)
  var myInitialState: AnswerPlaceholder.MyInitialState? = null

  @JsonProperty(INIT_FROM_DEPENDENCY)
  var myIsInitializedFromDependency = false

  @JsonProperty(POSSIBLE_ANSWER)
  var myPossibleAnswer = ""

  @JsonProperty(SELECTED)
  var mySelected = false

  @JsonProperty(STATUS)
  var myStatus = CheckStatus.Unchecked

  @JsonProperty(STUDENT_ANSWER)
  var myStudentAnswer: String? = null
}

class InitialStateMixin {
  @JsonProperty(LENGTH)
  var length = -1

  @JsonProperty(OFFSET)
  var offset = -1
}

class EduAnswerPlaceholderBuilder(
  @JsonProperty(INIT_FROM_DEPENDENCY) private val isInitializedFromDependency: Boolean,
  private val initialState: AnswerPlaceholder.MyInitialState,
  private val possibleAnswer: String,
  private val selected: Boolean,
  private val status: CheckStatus,
  private val studentAnswer: String? = null,
  offset: Int,
  length: Int,
  placeholderText: String,
  dependency: AnswerPlaceholderDependency?
) : AnswerPlaceholderBuilder(offset, length, placeholderText, dependency) {
  override fun createPlaceholder(): AnswerPlaceholder {
    val placeholder = super.createPlaceholder()
    placeholder.initialState = initialState
    placeholder.isInitializedFromDependency = isInitializedFromDependency
    placeholder.possibleAnswer = possibleAnswer
    placeholder.selected = selected
    placeholder.status = status
    placeholder.studentAnswer = studentAnswer
    return placeholder
  }
}
