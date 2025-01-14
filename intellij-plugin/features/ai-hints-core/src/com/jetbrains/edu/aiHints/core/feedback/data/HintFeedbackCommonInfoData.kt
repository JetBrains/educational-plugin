package com.jetbrains.edu.aiHints.core.feedback.data

import com.jetbrains.edu.aiHints.core.messages.EduAIHintsCoreBundle
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import kotlinx.serialization.Serializable

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