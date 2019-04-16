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
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.BrowserHyperlinkListener
import com.intellij.ui.JBColor
import com.intellij.ui.components.labels.ActionLink
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
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
import com.jetbrains.edu.learning.statistics.EduUsagesCollector
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.ui.taskDescription.TaskDescriptionToolWindowFactory
import com.jetbrains.edu.learning.ui.taskDescription.createTextPane
import com.jetbrains.edu.learning.ui.taskDescription.styleManagers.StyleManager
import kotlinx.css.CSSBuilder
import kotlinx.css.body
import java.awt.BorderLayout
import javax.swing.*

private const val MAX_MESSAGE_LENGTH = 400
private const val MAX_EXPECTED_ACTUAL_LENGTH = 150

class CheckDetailsPanel(project: Project, task: Task, checkResult: CheckResult) : JPanel(BorderLayout()) {
  init {
    val linksPanel = createLinksPanel(project, checkResult, task)
    val messagePanel = createMessagePanel(project, checkResult, linksPanel)

    // hack to hide empty border if there are no check details to show
    isVisible = !messagePanel.isEmpty || !linksPanel.components.isEmpty()
    border = JBUI.Borders.empty(20, 0, 0, 0)
    add(messagePanel, BorderLayout.CENTER)
    add(linksPanel, BorderLayout.SOUTH)
  }

  private fun createMessagePanel(project: Project, checkResult: CheckResult, linksPanel: JPanel): CheckMessagePanel {
    val messagePanel = CheckMessagePanel.create(checkResult)
    val details = checkResult.details
    if (details != null) {
      when (checkResult.message) {
        CheckUtils.COMPILATION_FAILED_MESSAGE -> CheckDetailsView.getInstance(project).showCompilationResults(details)
        CheckUtils.FAILED_TO_CHECK_MESSAGE -> CheckDetailsView.getInstance(project).showFailedToCheckMessage(details)
      }
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

    if (task.course is HyperskillCourse && checkResult.status == CheckStatus.Failed) {
      val showMoreInfo = LightColoredActionLink("Review Topics for the Stage...", SwitchTaskTabAction(project, 1))
      linksPanel.add(showMoreInfo, BorderLayout.SOUTH)
    }

    if (task.course !is CourseraCourse) {
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

    if (EduUtils.isStudentProject(project) && task.canShowSolution()) {
      val peekSolution = LightColoredActionLink("Peek Solution...",
                                                ActionManager.getInstance().getAction(CompareWithAnswerAction.ACTION_ID))
      answerHintsPanel.value.add(peekSolution)
    }

    if (checkResult.diff != null) {
      val compareOutputs = LightColoredActionLink("Compare Outputs...", CompareOutputsAction(project, checkResult.diff))
      answerHintsPanel.value.add(compareOutputs)
    }
    return if (answerHintsPanel.isInitialized()) answerHintsPanel.value else null
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
      EduUsagesCollector.fullOutputShown()
    }
  }

  class SwitchTaskTabAction(private val project: Project, private val index: Int): DumbAwareAction(null) {
    override fun actionPerformed(e: AnActionEvent) {
      val window = ToolWindowManager.getInstance(project).getToolWindow(TaskDescriptionToolWindowFactory.STUDY_TOOL_WINDOW)
      val tab = window.contentManager.getContent(index)
      if (tab != null) {
        window.contentManager.setSelectedContent(tab)
        if (index == 1) {
          EduUsagesCollector.reviewStageTopics()
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

private class CheckMessagePanel private constructor(): JPanel() {

  private val messagePane: JTextPane = createTextPane().apply {
    addHyperlinkListener(BrowserHyperlinkListener.INSTANCE)
  }

  init {
    layout = BoxLayout(this, BoxLayout.Y_AXIS)
    border = JBUI.Borders.emptyLeft(FOCUS_BORDER_WIDTH)
    add(messagePane)
  }

  val isEmpty: Boolean get() = messagePane.document.getText(0, messagePane.document.length).isEmpty() && componentCount == 1

  private fun setMessage(message: String) {
    val displayMessage = if (message.length > MAX_MESSAGE_LENGTH) message.substring(0, MAX_MESSAGE_LENGTH) + "..." else message
    messagePane.text = displayMessage
  }

  private fun setDiff(diff: CheckResultDiff) {
    val (_, message, expectedText, actualText) = diff
    setMessage(message)
    val expected = createLabeledComponent(expectedText, "Expected", 16)
    val actual = createLabeledComponent(actualText, "Actual", 8)
    UIUtil.mergeComponentsWithAnchor(expected, actual)

    add(expected)
    add(actual)
  }

  private fun createLabeledComponent(resultText: String, labelText: String, topPadding: Int): LabeledComponent<JComponent> {
    val text = if (resultText.length > MAX_EXPECTED_ACTUAL_LENGTH) resultText.substring(0, MAX_EXPECTED_ACTUAL_LENGTH) + "..." else resultText
    val textPane = createTextPane()
    textPane.text = text.escapeHtmlEntities().monospaced()

    val labeledComponent = LabeledComponent.create<JComponent>(textPane, labelText, BorderLayout.WEST)
    labeledComponent.label.foreground = UIUtil.getLabelDisabledForeground()
    labeledComponent.label.verticalAlignment = JLabel.TOP
    labeledComponent.border = JBUI.Borders.emptyTop(topPadding)
    return labeledComponent
  }

  companion object {
    private val FOCUS_BORDER_WIDTH = if (SystemInfo.isMac) 3 else if (SystemInfo.isWindows) 0 else 2

    fun create(checkResult: CheckResult): CheckMessagePanel {
      val messagePanel = CheckMessagePanel()
      messagePanel.setMessage(checkResult.escapedMessage)
      if (checkResult.diff != null) {
        messagePanel.setDiff(checkResult.diff)
      }
      return messagePanel
    }
  }
}

private fun String.escapeHtmlEntities(): String {
  return StringUtil.replace(this,
                            listOf("<", ">", "&", "'", "\"", " ", "\n"),
                            listOf("&lt;", "&gt;", "&amp;", "&#39;", "&quot;", "&nbsp;", "<br>"))
}

private fun String.monospaced(): String {
  val fontCss = CSSBuilder().apply {
    body {
      fontFamily = StyleManager().codeFont
    }
  }.toString()
  return "<html><head><style>${fontCss}</style></head><body>${this}</body></html>"
}
