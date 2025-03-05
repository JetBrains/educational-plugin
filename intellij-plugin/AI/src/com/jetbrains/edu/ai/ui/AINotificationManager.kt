package com.jetbrains.edu.ai.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowFactory
import javax.swing.BoxLayout

abstract class AINotificationManager<T : AINotification>(protected val project: Project) {
  protected abstract fun getNotifications(toolWindow: ToolWindow): List<T>

  fun closeExistingNotifications() {
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TaskToolWindowFactory.STUDY_TOOL_WINDOW) ?: return
    val existingNotifications = getNotifications(toolWindow)
    existingNotifications.forEach { it.close() }
  }

  protected fun showNotification(toolWindow: ToolWindow, notification: T) {
    toolWindow.component.add(notification, BoxLayout.Y_AXIS)
  }
}