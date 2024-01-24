@file:Suppress("unused")

package com.jetbrains.edu.learning.yaml.format.student

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholderDependency
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.json.encrypt.Encrypt
import com.jetbrains.edu.learning.yaml.format.AnswerPlaceholderBuilder
import com.jetbrains.edu.learning.yaml.format.AnswerPlaceholderYamlMixin
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.DEPENDENCY
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.ENCRYPTED_POSSIBLE_ANSWER
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.INITIAL_STATE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.INIT_FROM_DEPENDENCY
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.IS_VISIBLE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.LENGTH
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.OFFSET
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.PLACEHOLDER_TEXT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.SELECTED
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.STATUS
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.STUDENT_ANSWER

@Suppress("unused") // used for yaml serialization
@JsonDeserialize(builder = EduAnswerPlaceholderBuilder::class)
@JsonPropertyOrder(OFFSET, LENGTH, PLACEHOLDER_TEXT, DEPENDENCY, INITIAL_STATE, INIT_FROM_DEPENDENCY, ENCRYPTED_POSSIBLE_ANSWER, SELECTED, STATUS,
                   STUDENT_ANSWER, IS_VISIBLE)
abstract class StudentAnswerPlaceholderYamlMixin : AnswerPlaceholderYamlMixin() {

  @JsonProperty(INITIAL_STATE)
  private var _initialState: AnswerPlaceholder.MyInitialState? = null

  @JsonProperty(INIT_FROM_DEPENDENCY)
  private var isInitializedFromDependency = false

  @JsonProperty(ENCRYPTED_POSSIBLE_ANSWER)
  @Encrypt
  private var possibleAnswer = ""

  @JsonProperty(SELECTED)
  private var selected = false

  @JsonProperty(STATUS)
  private var status = CheckStatus.Unchecked

  @JsonProperty(STUDENT_ANSWER)
  private var studentAnswer: String? = null
}

class InitialStateMixin {
  @JsonProperty(LENGTH)
  private var length = -1

  @JsonProperty(OFFSET)
  private var offset = -1
}

class EduAnswerPlaceholderBuilder(
  @JsonProperty(INIT_FROM_DEPENDENCY) private val isInitializedFromDependency: Boolean,
  private val initialState: AnswerPlaceholder.MyInitialState,
  private val possibleAnswer: String = "",
  @Encrypt @JsonProperty(ENCRYPTED_POSSIBLE_ANSWER) private val encryptedPossibleAnswer: String?,
  private val selected: Boolean,
  private val status: CheckStatus,
  private val studentAnswer: String? = null,
  offset: Int,
  length: Int,
  placeholderText: String,
  dependency: AnswerPlaceholderDependency?,
  isVisible: Boolean
) : AnswerPlaceholderBuilder(offset, length, placeholderText, dependency, isVisible) {
  override fun createPlaceholder(): AnswerPlaceholder {
    val placeholder = super.createPlaceholder()
    placeholder.initialState = initialState
    placeholder.isInitializedFromDependency = isInitializedFromDependency
    placeholder.possibleAnswer = encryptedPossibleAnswer ?: possibleAnswer
    placeholder.selected = selected
    placeholder.status = status
    placeholder.studentAnswer = studentAnswer
    return placeholder
  }
}
