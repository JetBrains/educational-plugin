@file:UseSerializers(TranslationLanguageKSerializer::class, TranslationVersionKSerializer::class)

package com.jetbrains.edu.ai.translation.feedback

import com.jetbrains.edu.ai.messages.EduAIBundle
import com.jetbrains.edu.learning.ai.TranslationProperties
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.educational.core.format.enum.TranslationLanguage
import com.jetbrains.educational.translation.format.domain.TranslationVersion
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class AITranslationFeedbackInfoData(
  val courseId: Int,
  val courseUpdateVersion: Int,
  val courseName: String,
  val taskId: Int,
  val taskName: String,
  val translationLanguage: TranslationLanguage,
  val translationVersion: TranslationVersion
) {
  override fun toString(): String =
    buildString {
      appendLine(EduCoreBundle.message("ui.feedback.dialog.system.info.course.id"))
      appendLine(courseId)
      appendLine(EduAIBundle.message("ai.translation.share.feedback.dialog.system.info.data.course.update.version"))
      appendLine(courseUpdateVersion)
      appendLine(EduCoreBundle.message("ui.feedback.dialog.system.info.course.name"))
      appendLine(courseName)
      appendLine(EduCoreBundle.message("ui.feedback.dialog.system.info.task.id"))
      appendLine(taskId)
      appendLine(EduCoreBundle.message("ui.feedback.dialog.system.info.task.name"))
      appendLine(taskName)
      appendLine(EduAIBundle.message("ai.translation.share.feedback.dialog.info.data.translation.language"))
      appendLine(translationLanguage)
      appendLine(EduAIBundle.message("ai.translation.share.feedback.dialog.info.data.translation.version"))
      appendLine(translationVersion)
    }

  companion object {
    fun from(course: Course, task: Task, translationProperties: TranslationProperties): AITranslationFeedbackInfoData =
      AITranslationFeedbackInfoData(
        courseId = course.id,
        courseUpdateVersion = course.marketplaceCourseVersion,
        courseName = course.name,
        taskId = task.id,
        taskName = task.name,
        translationLanguage = translationProperties.language,
        translationVersion = translationProperties.version
      )
  }
}