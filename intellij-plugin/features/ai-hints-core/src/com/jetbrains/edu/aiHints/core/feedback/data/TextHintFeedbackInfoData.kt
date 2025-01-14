@file:UseSerializers(TextHintKSerializer::class)

package com.jetbrains.edu.aiHints.core.feedback.data

import com.jetbrains.edu.aiHints.core.messages.EduAIHintsCoreBundle
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.educational.ml.hints.hint.TextHint
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class TextHintFeedbackInfoData(
  val hintFeedbackInfoData: HintFeedbackCommonInfoData,
  val textHint: TextHint,
) {
  override fun toString(): String = buildString {
    appendLine(hintFeedbackInfoData.toString())
    appendLine(EduAIHintsCoreBundle.message("hints.feedback.label.text.hint"))
    appendLine(textHint)
  }

  companion object {
    @JvmStatic
    fun create(
      course: Course,
      task: Task,
      studentSolution: String,
      textHint: TextHint,
    ): TextHintFeedbackInfoData = TextHintFeedbackInfoData(
      HintFeedbackCommonInfoData.create(course, task, studentSolution),
      textHint = textHint,
    )
  }
}