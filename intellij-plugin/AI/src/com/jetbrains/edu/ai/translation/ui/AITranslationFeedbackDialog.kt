package com.jetbrains.edu.ai.translation.ui

import com.intellij.openapi.project.Project
import com.intellij.platform.feedback.dialog.BlockBasedFeedbackDialog
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
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.notification.EduNotificationManager

class AITranslationFeedbackDialog(
  private val project: Project,
  private val course: EduCourse,
  private val task: Task,
  private val translationProperties: TranslationProperties,
) : BlockBasedFeedbackDialog<AITranslationFeedbackSystemInfoData>(project, false) {
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

  override val myShowFeedbackSystemInfoDialog: () -> Unit = {
    showFeedbackSystemInfoDialog(project, mySystemInfoData.commonSystemInfo) {
      row(EduCoreBundle.message("ui.feedback.dialog.system.info.course.id")) {
        label(mySystemInfoData.aiTranslationFeedbackInfoData.courseId.toString())
      }
      row(EduAIBundle.message("ai.translation.share.feedback.dialog.system.info.data.course.update.version")) {
        label(mySystemInfoData.aiTranslationFeedbackInfoData.courseUpdateVersion.toString())
      }
      row(EduCoreBundle.message("ui.feedback.dialog.system.info.course.name")) {
        label(mySystemInfoData.aiTranslationFeedbackInfoData.courseName)
      }
      row(EduCoreBundle.message("ui.feedback.dialog.system.info.task.id")) {
        label(mySystemInfoData.aiTranslationFeedbackInfoData.taskId.toString())
      }
      row(EduCoreBundle.message("ui.feedback.dialog.system.info.task.name")) {
        label(mySystemInfoData.aiTranslationFeedbackInfoData.taskName)
      }
      row(EduAIBundle.message("ai.translation.share.feedback.dialog.info.data.translation.language")) {
        label(mySystemInfoData.aiTranslationFeedbackInfoData.translationLanguage.label)
      }
      row(EduAIBundle.message("ai.translation.share.feedback.dialog.info.data.translation.version")) {
        label(mySystemInfoData.aiTranslationFeedbackInfoData.translationVersion.value.toString())
      }
    }
  }

  override val mySystemInfoData: AITranslationFeedbackSystemInfoData by lazy {
    AITranslationFeedbackSystemInfoData(
      CommonFeedbackSystemData.getCurrentData(),
      AITranslationFeedbackInfoData.from(course, task, translationProperties),
    )
  }

  override fun showThanksNotification() {
    EduNotificationManager.showInfoNotification(
      project = project,
      title = EduAIBundle.message("ai.translation.share.feedback.notification.title"),
      content = EduAIBundle.message("ai.translation.share.feedback.notification.content")
    )
  }

  init {
    init()
  }
}