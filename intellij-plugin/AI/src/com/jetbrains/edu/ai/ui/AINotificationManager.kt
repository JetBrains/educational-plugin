package com.jetbrains.edu.ai.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowFactory
import javax.swing.BoxLayout

abstract class AINotificationManager<T : AINotification> {
  protected inline fun <reified T : AINotification> closeExistingNotifications(project: Project) {
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TaskToolWindowFactory.STUDY_TOOL_WINDOW) ?: return
    closeExistingNotifications<T>(toolWindow)
  }

  protected fun showNotification(toolWindow: ToolWindow, notification: T) {
    toolWindow.component.add(notification, BoxLayout.Y_AXIS)
  }

  protected inline fun <reified T : AINotification> closeExistingNotifications(toolWindow: ToolWindow) {
    val existingNotifications = toolWindow.component.components.filterIsInstance<T>()
    existingNotifications.forEach { it.close() }
  }
}