package com.jetbrains.edu.learning.ui.taskDescription.check

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.EduUtils
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

    var message = checkResult.details ?: checkResult.message
    if (message.length > 150) {
      message = message.substring(0, 150) + "..."
    }
    taskTextTw.setText(StringUtil.escapeXml(message))
  }
}