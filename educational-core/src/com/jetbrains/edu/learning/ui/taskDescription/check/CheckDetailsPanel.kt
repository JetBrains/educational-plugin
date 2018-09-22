package com.jetbrains.edu.learning.ui.taskDescription.check

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.JBColor
import com.intellij.ui.components.labels.ActionLink
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.actions.CompareWithAnswerAction
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.ui.taskDescription.JavaFxToolWindow
import com.jetbrains.edu.learning.ui.taskDescription.SwingToolWindow
import java.awt.BorderLayout
import javax.swing.JPanel

class CheckDetailsPanel(project: Project, checkResult: CheckResult) : JPanel(BorderLayout()) {
  init {
    border = JBUI.Borders.empty(20, 0, 0, 0)
    val taskTextTw = if (EduUtils.hasJavaFx() && EduSettings.getInstance().shouldUseJavaFx()) JavaFxToolWindow() else SwingToolWindow()
    val messagePanel = taskTextTw.createTaskInfoPanel(project)
    messagePanel.preferredSize = JBUI.size(100, 100)
    add(messagePanel, BorderLayout.CENTER)

    val peekSolution = LightColoredActionLink("Peek Solution...", CompareWithAnswerAction.ACTION_ID)
    val linksPanel = JPanel(BorderLayout())
    add(linksPanel, BorderLayout.SOUTH)

    linksPanel.add(peekSolution, BorderLayout.CENTER)

    var message = checkResult.details ?: checkResult.message
    if (message.length > 150) {
      message = message.substring(0, 150) + "..."
      linksPanel.add(LightColoredActionLink("Show Full Output...", CompareWithAnswerAction.ACTION_ID), BorderLayout.NORTH)
    }
    taskTextTw.setText(StringUtil.escapeXml(message))

  }

  private class LightColoredActionLink(text: String, actionId: String): ActionLink(text, ActionManager.getInstance().getAction(actionId)) {
    init {
      setNormalColor(JBColor(0x6894C6, 0x5C84C9))
      border = JBUI.Borders.empty(16, 0, 0, 0)
    }
  }
}