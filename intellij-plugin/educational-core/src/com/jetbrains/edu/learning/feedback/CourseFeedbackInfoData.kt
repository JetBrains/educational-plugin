package com.jetbrains.edu.learning.feedback

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.languageDisplayName
import com.jetbrains.edu.learning.messages.EduCoreBundle
import kotlinx.serialization.Serializable

@Serializable
data class CourseFeedbackInfoData(
  val courseName: String,
  val courseLanguage: String,
  val isStudent: Boolean,
  val courseId: Int
) {

  override fun toString(): String {
    return buildString {
      appendLine(EduCoreBundle.message("ui.feedback.dialog.system.info.course.info"))
      appendLine()
      appendLine(EduCoreBundle.message("ui.feedback.dialog.system.info.course.mode"))
      appendLine(
        if (isStudent) EduCoreBundle.message("ui.feedback.dialog.system.info.course.mode.student")
        else EduCoreBundle.message("ui.feedback.dialog.system.info.course.mode.teacher")
      )
      appendLine(EduCoreBundle.message("ui.feedback.dialog.system.info.course.name"))
      appendLine(courseName)
      appendLine(EduCoreBundle.message("ui.feedback.dialog.system.info.course.language"))
      appendLine(courseLanguage)
    }
  }

  companion object {
    fun from(course: Course, name: String? = null): CourseFeedbackInfoData {
      return CourseFeedbackInfoData(name ?: course.name, course.languageDisplayName, course.isStudy, course.id)
    }
  }
}
