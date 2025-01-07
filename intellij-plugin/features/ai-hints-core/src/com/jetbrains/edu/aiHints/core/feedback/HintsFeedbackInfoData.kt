@file:UseSerializers(TextHintSerializer::class, CodeHintSerializer::class)

package com.jetbrains.edu.aiHints.core.feedback

import com.jetbrains.edu.aiHints.core.messages.EduAIHintsCoreBundle
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.educational.ml.hints.hint.CodeHint
import com.jetbrains.educational.ml.hints.hint.TextHint
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class HintsFeedbackInfoData(
  val courseId: Int,
  val courseUpdateVersion: Int,
  val courseName: String,
  val taskId: Int,
  val taskName: String,
  val studentSolution: String,
  val textHint: TextHint,
  val codeHint: CodeHint,
) {
  override fun toString(): String = buildString {
    appendLine(EduCoreBundle.message("ui.feedback.dialog.system.info.course.id"))
    appendLine(courseId)
    appendLine(EduAIHintsCoreBundle.message("hints.feedback.label.course.update.version"))
    appendLine(courseUpdateVersion)
    appendLine(EduCoreBundle.message("ui.feedback.dialog.system.info.course.name"))
    appendLine(courseName)
    appendLine(EduAIHintsCoreBundle.message("hints.feedback.label.task.id"))
    appendLine(taskId)
    appendLine(EduAIHintsCoreBundle.message("hints.feedback.label.task.name"))
    appendLine(taskName)
    appendLine(EduAIHintsCoreBundle.message("hints.feedback.label.student.solution"))
    appendLine(studentSolution)
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
    ): HintsFeedbackInfoData = HintsFeedbackInfoData(
      courseId = course.id,
      courseUpdateVersion = course.marketplaceCourseVersion,
      courseName = course.name,
      taskId = task.id,
      taskName = task.name,
      studentSolution = studentSolution,
      textHint = textHint,
      codeHint = codeHint
    )
  }
}