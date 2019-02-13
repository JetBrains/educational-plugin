package com.jetbrains.edu.learning.ui.taskDescription.check

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.BrowserHyperlinkListener
import com.intellij.ui.JBColor
import com.intellij.ui.components.labels.ActionLink
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.actions.CompareWithAnswerAction
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.checker.details.CheckDetailsView
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.canShowSolution
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.coursera.CourseraCourse
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.ui.taskDescription.TaskDescriptionToolWindowFactory
import com.jetbrains.edu.learning.ui.taskDescription.createTextPane
import com.jetbrains.edu.learning.ui.taskDescription.styleManagers.StyleManager
import kotlinx.css.CSSBuilder
import kotlinx.css.body
import java.awt.BorderLayout
import javax.swing.Icon
import javax.swing.JEditorPane
import javax.swing.JPanel
import javax.swing.JTextPane

class CheckDetailsPanel(project: Project, task: Task, checkResult: CheckResult) : JPanel(BorderLayout()) {
  init {
    val linksPanel = createLinksPanel(task, checkResult, project)
    val messagePanel = createMessagePanel(checkResult, project, linksPanel)

    // hack to hide empty border if there are no check details to show
    isVisible = !messagePanel.plainText().isEmpty() || !linksPanel.components.isEmpty()
    border = JBUI.Borders.empty(20, 0, 0, 0)
    add(messagePanel, BorderLayout.CENTER)
    add(linksPanel, BorderLayout.SOUTH)
  }

  private fun createMessagePanel(checkResult: CheckResult,
                                 project: Project,
                                 linksPanel: JPanel): JTextPane {
    val messagePanel = createTextPane()
    messagePanel.addHyperlinkListener(BrowserHyperlinkListener.INSTANCE)
    val details = checkResult.details
    if (details != null) {
      when (checkResult.message) {
        CheckUtils.COMPILATION_FAILED_MESSAGE -> CheckDetailsView.getInstance(project).showCompilationResults(details)
        CheckUtils.FAILED_TO_CHECK_MESSAGE -> CheckDetailsView.getInstance(project).showFailedToCheckMessage(details)
      }
    }
    var message = checkResult.escapedMessage
    if (message.length > 400) {
      message = message.substring(0, 400) + "..."
      linksPanel.add(LightColoredActionLink("Show Full Output...", ShowFullOutputAction(project, details ?: checkResult.message)),
                     BorderLayout.NORTH)
    }
    messagePanel.text = messageWithFontStyle(message)
    messagePanel.margin.left = JBUI.scale(FOCUS_BORDER_WIDTH)
    return messagePanel
  }

  private fun messageWithFontStyle(message: String): String {
    val result = message.replace("\n", "<br>")
    val fontCss = CSSBuilder().apply {
      body {
        fontFamily = StyleManager().codeFont
      }
    }.toString()
    return "<html><head><style>${fontCss}</style></head><body>${result}</body></html>"
  }

  private fun createLinksPanel(task: Task,
                               checkResult: CheckResult,
                               project: Project): JPanel {
    val linksPanel = JPanel(BorderLayout())
    linksPanel.border = JBUI.Borders.emptyLeft(2)

    if (task.course is HyperskillCourse && checkResult.status == CheckStatus.Failed) {
      val showMoreInfo = LightColoredActionLink("Review Topics for the Stage...", SwitchTaskTabAction(project, 1))
      linksPanel.add(showMoreInfo, BorderLayout.SOUTH)
    }

    if (EduUtils.isStudentProject(project) && task.course !is CourseraCourse && task.canShowSolution()) {
      val peekSolution = LightColoredActionLink("Peek Solution...",
                                                ActionManager.getInstance().getAction(CompareWithAnswerAction.ACTION_ID))
      linksPanel.add(peekSolution, BorderLayout.CENTER)
    }
    return linksPanel
  }

  private fun JEditorPane.plainText() = document.getText(0, document.length)

  class LightColoredActionLink(text: String, action: AnAction, icon: Icon? = null): ActionLink(text, icon, action) {
    init {
      setNormalColor(JBColor(0x6894C6, 0x5C84C9))
      border = JBUI.Borders.empty(16, 0, 0, 0)
    }
  }

  private class ShowFullOutputAction(private val project: Project, private val text: String): DumbAwareAction(null) {
    override fun actionPerformed(e: AnActionEvent) {
      CheckDetailsView.getInstance(project).showOutput(text)
    }
  }

  class SwitchTaskTabAction(private val project: Project, private val index: Int): DumbAwareAction(null) {
    override fun actionPerformed(e: AnActionEvent) {
      val window = ToolWindowManager.getInstance(project).getToolWindow(TaskDescriptionToolWindowFactory.STUDY_TOOL_WINDOW)
      val tab = window.contentManager.getContent(index)
      if (tab != null) {
        window.contentManager.setSelectedContent(tab)
      }
    }
  }

  companion object {
    private val FOCUS_BORDER_WIDTH = if (SystemInfo.isMac) 3 else if (SystemInfo.isWindows) 0 else 2
  }
}