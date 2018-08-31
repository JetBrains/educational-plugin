package com.jetbrains.edu.learning.ui.taskDescription

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.Project
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.ui.taskDescription.check.CheckResultLabel
import java.awt.BorderLayout
import javax.swing.JPanel


class TaskDescriptionCheckListener: CheckListener {
  override fun beforeCheck(project: Project, task: Task) {
    val toolWindow = TaskDescriptionPanel.getToolWindow(project)!!
    toolWindow.setDefaultStateForMiddlePanel()
    toolWindow.icon.isVisible = true
  }

  override fun afterCheck(project: Project, task: Task, result: CheckResult) {
    val toolWindow = TaskDescriptionPanel.getToolWindow(project)
    toolWindow!!.icon.isVisible = false
    toolWindow.middlePanel.removeAll()
    toolWindow.middlePanel.add(CheckResultLabel(result), BorderLayout.WEST)
    toolWindow.middlePanel.add(JPanel(), BorderLayout.CENTER)
    UIUtil.setBackgroundRecursively(toolWindow.middlePanel, EditorColorsManager.getInstance().globalScheme.defaultBackground)
  }
}