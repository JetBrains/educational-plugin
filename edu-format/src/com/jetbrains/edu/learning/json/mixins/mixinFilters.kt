package com.jetbrains.edu.learning.json.mixins

import com.fasterxml.jackson.annotation.JsonInclude
import com.jetbrains.edu.learning.courseFormat.message

/**
 * Not to serialize default feedback_correct values
 */
@Suppress("EqualsOrHashCode")
class FeedbackCorrectFilter {
  override fun equals(other: Any?): Boolean = (other is String &&
                                               (other == message("check.correct.solution")))

}

/**
 * Not to serialize default feedback_incorrect values
 */
@Suppress("EqualsOrHashCode")
class FeedbackIncorrectFilter {
  override fun equals(other: Any?): Boolean = (other is String &&
                                               (other == message("check.incorrect.solution")))
}

/**
 * Not to serialize default quizHeader values
 */
@Suppress("EqualsOrHashCode")
class QuizHeaderFilter {
  override fun equals(other: Any?): Boolean = (other is String &&
                                               (other == message("course.creator.create.choice.task.multiple.label") ||
                                                other == message("course.creator.create.choice.task.single.label")))
}

/**
 * It's supposed to be used not to serialize `true` value when it's a default value via [JsonInclude] annotation API
 */
@Suppress("EqualsOrHashCode")
class TrueValueFilter {
  override fun equals(other: Any?): Boolean = other is Boolean && other
}

/**
 * It's supposed to be used not to serialize `0` value when it's a default value via [JsonInclude] annotation API
 */
@Suppress("EqualsOrHashCode")
class IntValueFilter {
  private val defaultValue: Int = 0

  override fun equals(other: Any?): Boolean = other is Int && other == defaultValue
}
