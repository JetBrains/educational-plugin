package com.jetbrains.edu.aiHints.core.feedback

import com.intellij.openapi.project.Project
import com.intellij.platform.feedback.dialog.BlockBasedFeedbackDialog
import com.intellij.platform.feedback.dialog.CommonFeedbackSystemData
import com.intellij.platform.feedback.dialog.showFeedbackSystemInfoDialog
import com.intellij.platform.feedback.dialog.uiBlocks.FeedbackBlock
import com.intellij.platform.feedback.dialog.uiBlocks.TextAreaBlock
import com.jetbrains.edu.ai.translation.ui.LikeBlock
import com.jetbrains.edu.aiHints.core.messages.EduAIHintsCoreBundle
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.educational.ml.hints.hint.CodeHint
import com.jetbrains.educational.ml.hints.hint.TextHint

class HintsFeedbackDialog(
  private val project: Project,
  private val course: Course,
  private val task: Task,
  private val studentSolution: String,
  private val textHint: TextHint,
  private val codeHint: CodeHint,
) : BlockBasedFeedbackDialog<HintsFeedbackSystemInfoData>(project, false) {

  override val myFeedbackReportId: String = "edu_ai_hints_feedback"

  override val myTitle: String
    get() = EduAIHintsCoreBundle.message("hints.feedback.dialog.title")

  override val myBlocks: List<FeedbackBlock> = listOf(
    LikeBlock(
      EduAIHintsCoreBundle.message("hints.feedback.like.label"),
      "hints_likeness"
    ),
    TextAreaBlock("", "hints_experience")
      .setPlaceholder(EduCoreBundle.message("ui.feedback.dialog.textarea.optional.label"))
  )

  override val myShowFeedbackSystemInfoDialog: () -> Unit = {
    showFeedbackSystemInfoDialog(project, mySystemInfoData.commonSystemInfo) {
      row(EduCoreBundle.message("ui.feedback.dialog.system.info.course.id")) {
        label(mySystemInfoData.hintsFeedbackInfo.courseId.toString())
      }
      row(EduCoreBundle.message("ui.feedback.dialog.system.info.course.update.version")) {
        label(mySystemInfoData.hintsFeedbackInfo.courseUpdateVersion.toString())
      }
      row(EduCoreBundle.message("ui.feedback.dialog.system.info.course.name")) {
        label(mySystemInfoData.hintsFeedbackInfo.courseName)
      }
      row(EduCoreBundle.message("ui.feedback.dialog.system.info.task.id")) {
        label(mySystemInfoData.hintsFeedbackInfo.taskId.toString())
      }
      row(EduCoreBundle.message("ui.feedback.dialog.system.info.task.name")) {
        label(mySystemInfoData.hintsFeedbackInfo.taskName)
      }
      row(EduAIHintsCoreBundle.message("hints.feedback.label.student.solution")) {
        label(mySystemInfoData.hintsFeedbackInfo.studentSolution)
      }
      row(EduAIHintsCoreBundle.message("hints.feedback.label.text.hint")) {
        label(mySystemInfoData.hintsFeedbackInfo.textHint.text)
      }
      row(EduAIHintsCoreBundle.message("hints.feedback.label.code.hint")) {
        label(mySystemInfoData.hintsFeedbackInfo.codeHint.code)
      }
    }
  }

  override val mySystemInfoData: HintsFeedbackSystemInfoData by lazy {
    HintsFeedbackSystemInfoData(
      CommonFeedbackSystemData.getCurrentData(),
      HintsFeedbackInfoData.create(course, task, studentSolution, textHint, codeHint)
    )
  }

  init {
    init()
  }

  override fun showThanksNotification() {
    EduNotificationManager.showInfoNotification(
      project = project,
      title = EduAIHintsCoreBundle.message("hints.feedback.notification.title"),
      content = EduAIHintsCoreBundle.message("hints.feedback.notification.text")
    )
  }
}