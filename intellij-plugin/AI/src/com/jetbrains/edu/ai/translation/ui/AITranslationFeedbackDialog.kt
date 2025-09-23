package com.jetbrains.edu.ai.translation.ui

import com.intellij.openapi.project.Project
import com.intellij.platform.feedback.dialog.CommonFeedbackSystemData
import com.intellij.platform.feedback.dialog.showFeedbackSystemInfoDialog
import com.intellij.platform.feedback.dialog.uiBlocks.FeedbackBlock
import com.intellij.platform.feedback.dialog.uiBlocks.TextAreaBlock
import com.jetbrains.edu.ai.messages.EduAIBundle
import com.jetbrains.edu.ai.translation.feedback.AITranslationFeedbackInfoData
import com.jetbrains.edu.ai.translation.feedback.AITranslationFeedbackSystemInfoData
import com.jetbrains.edu.learning.ai.TranslationProperties
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.feedback.BlockBasedFeedbackDialogInternal
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.notification.EduNotificationManager

class AITranslationFeedbackDialog(
  private val project: Project,
  private val course: EduCourse,
  private val task: Task,
  private val translationProperties: TranslationProperties,
) : BlockBasedFeedbackDialogInternal<AITranslationFeedbackSystemInfoData>(project, false) {
  override val myFeedbackReportId: String = "edu_ai_translation_feedback"

  override val myTitle: String
    get() = EduAIBundle.message("ai.translation.share.feedback.title")

  override val myBlocks: List<FeedbackBlock> = listOf(
    LikeBlock(
      EduAIBundle.message("ai.translation.share.feedback.how.would.you.rate.the.ai.translation"),
      "translation_likeness"
    ),
    TextAreaBlock("", "translation_experience")
      .setPlaceholder(EduCoreBundle.message("ui.feedback.dialog.textarea.optional.label"))
  )

  override fun showFeedbackDialogInternal(systemInfoData: AITranslationFeedbackSystemInfoData) {
    showFeedbackSystemInfoDialog(project, systemInfoData.commonSystemInfo) {
      row(EduCoreBundle.message("ui.feedback.dialog.system.info.course.id")) {
        label(systemInfoData.aiTranslationFeedbackInfoData.courseId.toString())
      }
      row(EduAIBundle.message("ai.translation.share.feedback.dialog.system.info.data.course.update.version")) {
        label(systemInfoData.aiTranslationFeedbackInfoData.courseUpdateVersion.toString())
      }
      row(EduCoreBundle.message("ui.feedback.dialog.system.info.course.name")) {
        label(systemInfoData.aiTranslationFeedbackInfoData.courseName)
      }
      row(EduCoreBundle.message("ui.feedback.dialog.system.info.task.id")) {
        label(systemInfoData.aiTranslationFeedbackInfoData.taskId.toString())
      }
      row(EduCoreBundle.message("ui.feedback.dialog.system.info.task.name")) {
        label(systemInfoData.aiTranslationFeedbackInfoData.taskName)
      }
      row(EduAIBundle.message("ai.translation.share.feedback.dialog.info.data.translation.language")) {
        label(systemInfoData.aiTranslationFeedbackInfoData.translationLanguage.label)
      }
      row(EduAIBundle.message("ai.translation.share.feedback.dialog.info.data.translation.version")) {
        label(systemInfoData.aiTranslationFeedbackInfoData.translationVersion.value.toString())
      }
    }
  }

  override fun showThanksNotification() {
    EduNotificationManager.showInfoNotification(
      project = project,
      title = EduAIBundle.message("ai.translation.share.feedback.notification.title"),
      content = EduAIBundle.message("ai.translation.share.feedback.notification.content")
    )
  }

  override fun computeSystemInfoDataInternal(): AITranslationFeedbackSystemInfoData {
    return AITranslationFeedbackSystemInfoData(
      CommonFeedbackSystemData.getCurrentData(),
      AITranslationFeedbackInfoData.from(course, task, translationProperties),
    )
  }

  init {
    init()
  }
}