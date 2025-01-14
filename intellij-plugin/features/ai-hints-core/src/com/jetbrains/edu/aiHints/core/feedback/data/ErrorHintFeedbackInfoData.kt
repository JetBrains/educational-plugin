package com.jetbrains.edu.aiHints.core.feedback.data

import com.jetbrains.edu.aiHints.core.messages.EduAIHintsCoreBundle
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import kotlinx.serialization.Serializable

@Serializable
data class ErrorHintFeedbackInfoData(
  val hintFeedbackInfoData: HintFeedbackCommonInfoData,
  val errorMessage: String,
) {
  override fun toString(): String = buildString {
    appendLine(hintFeedbackInfoData.toString())
    appendLine(EduAIHintsCoreBundle.message("hints.feedback.label.error.message"))
    appendLine(errorMessage)
  }

  companion object {
    @JvmStatic
    fun create(
      course: Course,
      task: Task,
      studentSolution: String,
      errorMessage: String
    ): ErrorHintFeedbackInfoData = ErrorHintFeedbackInfoData(
      HintFeedbackCommonInfoData.create(course, task, studentSolution),
      errorMessage = errorMessage,
    )
  }
}