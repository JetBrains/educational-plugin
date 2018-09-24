package com.jetbrains.edu.learning.ui.taskDescription.check

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.BrowserHyperlinkListener
import com.intellij.ui.JBColor
import com.intellij.ui.components.labels.ActionLink
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.actions.CompareWithAnswerAction
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.courseFormat.ext.canShowSolution
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.coursera.CourseraNames
import com.jetbrains.edu.learning.ui.taskDescription.createTextPane
import com.jetbrains.edu.learning.ui.taskDescription.textWithStyles
import java.awt.BorderLayout
import javax.swing.JPanel

class CheckDetailsPanel(project: Project, task: Task, checkResult: CheckResult) : JPanel(BorderLayout()) {
  init {
    border = JBUI.Borders.empty(20, 0, 0, 0)
    val messagePanel = createTextPane()
    messagePanel.addHyperlinkListener(BrowserHyperlinkListener.INSTANCE)
    add(messagePanel, BorderLayout.CENTER)

    val linksPanel = JPanel(BorderLayout())
    add(linksPanel, BorderLayout.SOUTH)

    if (CourseraNames.COURSE_TYPE != task.course.courseType && task.canShowSolution()) {
      val peekSolution = LightColoredActionLink("Peek Solution...",
                                                ActionManager.getInstance().getAction(CompareWithAnswerAction.ACTION_ID))
      linksPanel.add(peekSolution, BorderLayout.CENTER)
    }

    var message = checkResult.details ?: checkResult.message
    if (message.length > 400) {
      message = message.substring(0, 400) + "..."
      linksPanel.add(LightColoredActionLink("Show Full Output...", ShowFullOutputAction(project, checkResult.details ?: checkResult.message)), BorderLayout.NORTH)
    }
    messagePanel.text = textWithStyles(message.replace("\n", "<br>"))
    messagePanel.margin.left = JBUI.scale(FOCUS_BORDER_WIDTH)
  }

  private class LightColoredActionLink(text: String, action: AnAction): ActionLink(text, action) {
    init {
      setNormalColor(JBColor(0x6894C6, 0x5C84C9))
      border = JBUI.Borders.empty(16, 0, 0, 0)
    }
  }

  private class ShowFullOutputAction(private val project: Project, private val text: String): DumbAwareAction(null) {
    override fun actionPerformed(e: AnActionEvent) {
      CheckUtils.showTestResultsToolWindow(project, text)
    }
  }

  companion object {
    private val FOCUS_BORDER_WIDTH = if (SystemInfo.isMac) 3 else if (SystemInfo.isWindows) 0 else 2
  }
}