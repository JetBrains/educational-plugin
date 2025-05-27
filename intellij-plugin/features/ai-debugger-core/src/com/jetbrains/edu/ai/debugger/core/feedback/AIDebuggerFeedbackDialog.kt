package com.jetbrains.edu.ai.debugger.core.feedback

import com.intellij.openapi.project.Project
import com.intellij.platform.feedback.dialog.BlockBasedFeedbackDialog
import com.intellij.platform.feedback.dialog.CommonFeedbackSystemData
import com.intellij.platform.feedback.dialog.showFeedbackSystemInfoDialog
import com.intellij.platform.feedback.dialog.uiBlocks.FeedbackBlock
import com.intellij.platform.feedback.dialog.uiBlocks.TextAreaBlock
import com.jetbrains.edu.ai.debugger.core.messages.EduAIDebuggerCoreBundle
import com.jetbrains.edu.ai.debugger.core.service.TestInfo
import com.jetbrains.edu.ai.translation.ui.LikeBlock
import com.jetbrains.edu.ai.translation.ui.LikeBlock.FeedbackLikenessAnswer
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.educational.ml.debugger.dto.Breakpoint
import com.jetbrains.educational.ml.debugger.response.BreakpointHintDetails

class AIDebuggerFeedbackDialog(
  private val project: Project,
  private val task: Task,
  private val userSolution: Map<String, String>,
  private val testInfo: TestInfo,
  private val finalBreakpoints: List<Breakpoint>,
  private val intermediateBreakpoints: Map<String, List<Int>>,
  private val breakpointHints: List<BreakpointHintDetails>,
  defaultLikeness: FeedbackLikenessAnswer = FeedbackLikenessAnswer.NO_ANSWER
) : BlockBasedFeedbackDialog<AIDebuggerFeedbackCommonInfoData>(project, false) {

  override val myFeedbackReportId: String = "edu_ai_debugger_feedback"

  override val myShowFeedbackSystemInfoDialog: () -> Unit = {
    showFeedbackSystemInfoDialog(project, mySystemInfoData.commonSystemInfo) {
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
      row(EduAIDebuggerCoreBundle.message("ai.debugger.feedback.label.student.solution")) {
        label(mySystemInfoData.studentSolution.toString())
      }
      row(EduAIDebuggerCoreBundle.message("ai.debugger.feedback.label.test.name")) {
        label(mySystemInfoData.testName)
      }
      row(EduAIDebuggerCoreBundle.message("ai.debugger.feedback.label.test.error.message")) {
        label(mySystemInfoData.testErrorMessage)
      }
      row(EduAIDebuggerCoreBundle.message("ai.debugger.feedback.label.test.expected.output")) {
        label(mySystemInfoData.testExpectedOutput)
      }
      row(EduAIDebuggerCoreBundle.message("ai.debugger.feedback.label.test.text")) {
        label(mySystemInfoData.testText)
      }
      row(EduAIDebuggerCoreBundle.message("ai.debugger.feedback.label.final.breakpoints")) {
        label(mySystemInfoData.finalBreakpoints.toString())
      }
      row(EduAIDebuggerCoreBundle.message("ai.debugger.feedback.label.intermediate.breakpoints")) {
        label(mySystemInfoData.intermediateBreakpoints.toString())
      }
      row(EduAIDebuggerCoreBundle.message("ai.debugger.feedback.label.breakpoint.hints")) {
        label(mySystemInfoData.breakpointHints.toString())
      }
    }
  }

  override val mySystemInfoData: AIDebuggerFeedbackCommonInfoData by lazy {
    AIDebuggerFeedbackCommonInfoData.create(
      commonSystemInfo = CommonFeedbackSystemData.getCurrentData(),
      task = task,
      userSolution = userSolution,
      testInfo = testInfo,
      finalBreakpoints = finalBreakpoints,
      intermediateBreakpoints = intermediateBreakpoints,
      breakpointHints = breakpointHints,
    )
  }

  override val myTitle: String
    get() = EduAIDebuggerCoreBundle.message("ai.debugger.feedback.dialog.title")

  override val myBlocks: List<FeedbackBlock> = listOf(
    LikeBlock(
      EduAIDebuggerCoreBundle.message("ai.debugger.feedback.like.label"),
      "hints_likeness",
      defaultLikeness
    ),
    TextAreaBlock("", "hints_experience")
      .setPlaceholder(EduCoreBundle.message("ui.feedback.dialog.textarea.optional.label"))
  )

  fun getLikenessAnswer(): FeedbackLikenessAnswer? {
    return (myBlocks.firstOrNull() as? LikeBlock)?.answer
  }

  override fun showThanksNotification() {
    EduNotificationManager.showInfoNotification(
      project = project,
      title = EduAIDebuggerCoreBundle.message("ai.debugger.feedback.notification.title"),
      content = EduAIDebuggerCoreBundle.message("ai.debugger.feedback.notification.text")
    )
  }

  init {
    init()
  }
}