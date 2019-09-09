package com.jetbrains.edu.learning.ui.taskDescription.check

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffDialogHints
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.JBColor
import com.intellij.ui.components.labels.ActionLink
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.actions.CompareWithAnswerAction
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.CheckResultDiff
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.checker.details.CheckDetailsView
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.canShowSolution
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.coursera.CourseraCourse
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.stepik.hyperskill.HSPeekSolutionAction
import com.jetbrains.edu.learning.stepik.hyperskill.canShowHyperskillSolution
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.ui.taskDescription.TaskDescriptionToolWindowFactory
import com.jetbrains.edu.learning.ui.taskDescription.check.CheckMessagePanel.Companion.MAX_EXPECTED_ACTUAL_LENGTH
import com.jetbrains.edu.learning.ui.taskDescription.check.CheckMessagePanel.Companion.MAX_MESSAGE_LENGTH
import java.awt.BorderLayout
import javax.swing.BoxLayout
import javax.swing.Icon
import javax.swing.JPanel

class CheckDetailsPanel(project: Project, task: Task, checkResult: CheckResult) : JPanel(BorderLayout()) {
  init {
    val linksPanel = createLinksPanel(project, checkResult, task)
    val messagePanel = createMessagePanel(project, checkResult, linksPanel)

    // hack to hide empty border if there are no check details to show
    isVisible = !messagePanel.isEmpty || linksPanel.components.isNotEmpty()
    border = JBUI.Borders.empty(20, 0, 0, 0)
    add(messagePanel, BorderLayout.CENTER)
    add(linksPanel, BorderLayout.SOUTH)
  }

  private fun createMessagePanel(project: Project, checkResult: CheckResult, linksPanel: JPanel): CheckMessagePanel {
    val messagePanel = CheckMessagePanel.create(checkResult)
    val details = checkResult.details
    if (details != null && checkResult.message in CheckUtils.ERRORS) {
      CheckDetailsView.getInstance(project).showCheckResultDetails(checkResult.message, details)
    }

    val expectedActualTextLength = if (checkResult.diff != null) {
      maxOf(checkResult.diff.actual.length, checkResult.diff.expected.length)
    } else {
      0
    }

    val messageLength = (checkResult.diff?.message ?: checkResult.escapedMessage).length

    if (messageLength > MAX_MESSAGE_LENGTH || expectedActualTextLength > MAX_EXPECTED_ACTUAL_LENGTH) {
      linksPanel.add(LightColoredActionLink("Show Full Output...", ShowFullOutputAction(project, details ?: checkResult.message)),
                     BorderLayout.NORTH)
    }
    return messagePanel
  }

  private fun createLinksPanel(project: Project, checkResult: CheckResult, task: Task): JPanel {
    val linksPanel = JPanel(BorderLayout())
    linksPanel.border = JBUI.Borders.emptyLeft(2)

    val course = task.course
    if (course is HyperskillCourse && course.isTaskInProject(task) && checkResult.status == CheckStatus.Failed) {
      val showMoreInfo = LightColoredActionLink("Review Topics for the Stage...", SwitchTaskTabAction(project, 1))
      linksPanel.add(showMoreInfo, BorderLayout.SOUTH)
    }

    if (course !is CourseraCourse) {
      val answerHintsPanel = createAnswerHintsPanel(project, task, checkResult)
      if (answerHintsPanel != null) {
        linksPanel.add(answerHintsPanel, BorderLayout.CENTER)
      }
    }
    return linksPanel
  }

  private fun createAnswerHintsPanel(project: Project, task: Task, checkResult: CheckResult): JPanel? {
    val answerHintsPanel = lazy(LazyThreadSafetyMode.NONE) {
      val panel = JPanel()
      panel.layout = BoxLayout(panel, BoxLayout.X_AXIS)
      panel
    }

    if (EduUtils.isStudentProject(project) && (task.canShowSolution() || canShowHyperskillSolution(task))) {
      val peekSolution = LightColoredActionLink("Peek Solution...",
                                                ActionManager.getInstance().getAction(getPeekSolutionAction(task)))
      answerHintsPanel.value.add(peekSolution)
    }

    if (checkResult.diff != null) {
      val compareOutputs = LightColoredActionLink("Compare Outputs...", CompareOutputsAction(project, checkResult.diff))
      answerHintsPanel.value.add(compareOutputs)
    }
    return if (answerHintsPanel.isInitialized()) answerHintsPanel.value else null
  }

  private fun getPeekSolutionAction(task: Task) : String {
    val course = task.course
    if (course is HyperskillCourse && !course.isTaskInProject(task)) {
      return HSPeekSolutionAction.ACTION_ID
    }
    return CompareWithAnswerAction.ACTION_ID
  }

  class LightColoredActionLink(text: String, action: AnAction, icon: Icon? = null): ActionLink(text, icon, action) {
    init {
      setNormalColor(JBColor(0x6894C6, 0x5C84C9))
      border = JBUI.Borders.empty(16, 0, 0, 16)
    }
  }

  private class ShowFullOutputAction(private val project: Project, private val text: String): DumbAwareAction(null) {
    override fun actionPerformed(e: AnActionEvent) {
      CheckDetailsView.getInstance(project).showOutput(text)
      EduCounterUsageCollector.fullOutputShown()
    }
  }

  class SwitchTaskTabAction(private val project: Project, private val index: Int): DumbAwareAction(null) {
    override fun actionPerformed(e: AnActionEvent) {
      val window = ToolWindowManager.getInstance(project).getToolWindow(TaskDescriptionToolWindowFactory.STUDY_TOOL_WINDOW)
      val tab = window.contentManager.getContent(index)
      if (tab != null) {
        window.contentManager.setSelectedContent(tab)
        if (index == 1) {
          EduCounterUsageCollector.reviewStageTopics()
        }
      }
    }
  }

  private class CompareOutputsAction(private val project: Project, private val diff: CheckResultDiff): DumbAwareAction(null) {
    override fun actionPerformed(e: AnActionEvent) {
      val expected = DiffContentFactory.getInstance().create(diff.expected)
      val actual = DiffContentFactory.getInstance().create(diff.actual)
      val request = SimpleDiffRequest(diff.title, expected, actual, "Expected", "Actual")
      DiffManager.getInstance().showDiff(project, request, DiffDialogHints.FRAME)
    }
  }
}
