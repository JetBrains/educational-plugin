@file:UseSerializers(TextHintKSerializer::class, CodeHintKSerializer::class)

package com.jetbrains.edu.aiHints.core.feedback.data

import com.jetbrains.edu.aiHints.core.messages.EduAIHintsCoreBundle
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.educational.ml.hints.hint.CodeHint
import com.jetbrains.educational.ml.hints.hint.TextHint
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class CodeHintFeedbackInfoData(
  val hintFeedbackInfoData: HintFeedbackCommonInfoData,
  val textHint: TextHint,
  val codeHint: CodeHint,
) {
  override fun toString(): String = buildString {
    appendLine(hintFeedbackInfoData.toString())
    appendLine(EduAIHintsCoreBundle.message("hints.feedback.label.text.hint"))
    appendLine(textHint)
    appendLine(EduAIHintsCoreBundle.message("hints.feedback.label.code.hint"))
    appendLine(codeHint)
  }

  companion object {
    @JvmStatic
    fun create(
      course: Course,
      task: Task,
      studentSolution: String,
      textHint: TextHint,
      codeHint: CodeHint
    ): CodeHintFeedbackInfoData = CodeHintFeedbackInfoData(
      HintFeedbackCommonInfoData.create(course, task, studentSolution),
      textHint = textHint,
      codeHint = codeHint
    )
  }
}