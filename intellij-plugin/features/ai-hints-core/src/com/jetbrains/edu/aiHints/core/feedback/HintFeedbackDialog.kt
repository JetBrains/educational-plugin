package com.jetbrains.edu.aiHints.core.feedback

import com.intellij.openapi.project.Project
import com.intellij.platform.feedback.dialog.BlockBasedFeedbackDialog
import com.intellij.platform.feedback.dialog.CommonFeedbackSystemData
import com.intellij.platform.feedback.dialog.SystemDataJsonSerializable
import com.intellij.platform.feedback.dialog.showFeedbackSystemInfoDialog
import com.intellij.platform.feedback.dialog.uiBlocks.FeedbackBlock
import com.intellij.platform.feedback.dialog.uiBlocks.TextAreaBlock
import com.intellij.ui.dsl.builder.Panel
import com.jetbrains.edu.ai.translation.ui.LikeBlock
import com.jetbrains.edu.aiHints.core.messages.EduAIHintsCoreBundle
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.educational.ml.hints.hint.CodeHint
import com.jetbrains.educational.ml.hints.hint.TextHint

abstract class HintFeedbackDialog<T : SystemDataJsonSerializable>(
  private val project: Project
) : BlockBasedFeedbackDialog<T>(project, false) {
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

  override fun showThanksNotification() {
    EduNotificationManager.showInfoNotification(
      project = project,
      title = EduAIHintsCoreBundle.message("hints.feedback.notification.title"),
      content = EduAIHintsCoreBundle.message("hints.feedback.notification.text")
    )
  }

  protected fun Panel.commonFeedbackData(mySystemInfoData: HintFeedbackCommonInfoData) {
    row(EduCoreBundle.message("ui.feedback.dialog.system.info.course.id")) {
      label(mySystemInfoData.courseId.toString())
    }
    row(EduCoreBundle.message("ui.feedback.dialog.system.info.course.update.version")) {
      label(mySystemInfoData.courseUpdateVersion.toString())
    }
    row(EduCoreBundle.message("ui.feedback.dialog.system.info.course.name")) {
      label(mySystemInfoData.courseName)
    }
    row(EduCoreBundle.message("ui.feedback.dialog.system.info.task.id")) {
      label(mySystemInfoData.taskId.toString())
    }
    row(EduCoreBundle.message("ui.feedback.dialog.system.info.task.name")) {
      label(mySystemInfoData.taskName)
    }
    row(EduAIHintsCoreBundle.message("hints.feedback.label.student.solution")) {
      label(mySystemInfoData.studentSolution)
    }
  }
}

class CodeHintFeedbackDialog(
  private val project: Project,
  private val course: Course,
  private val task: Task,
  private val studentSolution: String,
  private val textHint: TextHint,
  private val codeHint: CodeHint,
) : HintFeedbackDialog<CodeHintFeedbackSystemInfoData>(project) {

  override val myShowFeedbackSystemInfoDialog: () -> Unit = {
    showFeedbackSystemInfoDialog(project, mySystemInfoData.commonSystemInfo) {
      commonFeedbackData(mySystemInfoData.codeHintFeedbackInfo.hintFeedbackInfoData)
      row(EduAIHintsCoreBundle.message("hints.feedback.label.text.hint")) {
        label(mySystemInfoData.codeHintFeedbackInfo.textHint.text)
      }
      row(EduAIHintsCoreBundle.message("hints.feedback.label.code.hint")) {
        label(mySystemInfoData.codeHintFeedbackInfo.codeHint.code)
      }
    }
  }

  override val mySystemInfoData: CodeHintFeedbackSystemInfoData by lazy {
    CodeHintFeedbackSystemInfoData(
      CommonFeedbackSystemData.getCurrentData(),
      CodeHintFeedbackInfoData.create(course, task, studentSolution, textHint, codeHint)
    )
  }

  init {
    init()
  }
}

class TextHintFeedbackDialog(
  private val project: Project,
  private val course: Course,
  private val task: Task,
  private val studentSolution: String,
  private val textHint: TextHint,
) : HintFeedbackDialog<TextHintFeedbackSystemInfoData>(project) {

  override val myShowFeedbackSystemInfoDialog: () -> Unit = {
    showFeedbackSystemInfoDialog(project, mySystemInfoData.commonSystemInfo) {
      commonFeedbackData(mySystemInfoData.textHintFeedbackInfo.hintFeedbackInfoData)
      row(EduAIHintsCoreBundle.message("hints.feedback.label.text.hint")) {
        label(mySystemInfoData.textHintFeedbackInfo.textHint.text)
      }
    }
  }

  override val mySystemInfoData: TextHintFeedbackSystemInfoData by lazy {
    TextHintFeedbackSystemInfoData(
      CommonFeedbackSystemData.getCurrentData(),
      TextHintFeedbackInfoData.create(course, task, studentSolution, textHint)
    )
  }

  init {
    init()
  }
}