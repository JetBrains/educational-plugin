@file:UseSerializers(TextHintKSerializer::class, CodeHintKSerializer::class)

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
data class HintFeedbackCommonInfoData(
  val courseId: Int,
  val courseUpdateVersion: Int,
  val courseName: String,
  val taskId: Int,
  val taskName: String,
  val studentSolution: String,
) {
  override fun toString(): String = buildString {
    appendLine(EduCoreBundle.message("ui.feedback.dialog.system.info.course.id"))
    appendLine(courseId)
    appendLine(EduCoreBundle.message("ui.feedback.dialog.system.info.course.update.version"))
    appendLine(courseUpdateVersion)
    appendLine(EduCoreBundle.message("ui.feedback.dialog.system.info.course.name"))
    appendLine(courseName)
    appendLine(EduCoreBundle.message("ui.feedback.dialog.system.info.task.id"))
    appendLine(taskId)
    appendLine(EduCoreBundle.message("ui.feedback.dialog.system.info.task.name"))
    appendLine(taskName)
    appendLine(EduAIHintsCoreBundle.message("hints.feedback.label.student.solution"))
    appendLine(studentSolution)
  }

  companion object {
    @JvmStatic
    fun create(
      course: Course,
      task: Task,
      studentSolution: String,
    ): HintFeedbackCommonInfoData = HintFeedbackCommonInfoData(
      courseId = course.id,
      courseUpdateVersion = course.marketplaceCourseVersion,
      courseName = course.name,
      taskId = task.id,
      taskName = task.name,
      studentSolution = studentSolution
    )
  }
}

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