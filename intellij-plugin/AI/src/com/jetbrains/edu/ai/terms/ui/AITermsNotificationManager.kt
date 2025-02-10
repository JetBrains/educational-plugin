package com.jetbrains.edu.ai.terms.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.NotificationContent
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.EditorNotificationPanel
import com.jetbrains.edu.ai.ui.AINotification.ActionLabel
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowFactory
import javax.swing.BoxLayout

object AITermsNotificationManager {
  fun showInfoNotification(project: Project, message: @NotificationContent String, actionLabel: ActionLabel? = null) {
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TaskToolWindowFactory.STUDY_TOOL_WINDOW) ?: return
    val notification = AITermsNotification(EditorNotificationPanel.Status.Info, message, toolWindow.component)
    if (actionLabel != null) {
      notification.addActionLabel(actionLabel)
    }
    closeExistingNotifications(toolWindow)
    showNotification(toolWindow, notification)
  }

  fun showErrorNotification(project: Project, message: @NotificationContent String, actionLabel: ActionLabel? = null) {
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TaskToolWindowFactory.STUDY_TOOL_WINDOW) ?: return
    val notification = AITermsNotification(EditorNotificationPanel.Status.Error, message, toolWindow.component)
    if (actionLabel != null) {
      notification.addActionLabel(actionLabel)
    }
    closeExistingNotifications(toolWindow)
    showNotification(toolWindow, notification)
  }

  fun closeExistingNotifications(project: Project) {
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TaskToolWindowFactory.STUDY_TOOL_WINDOW) ?: return
    closeExistingNotifications(toolWindow)
  }

  private fun showNotification(toolWindow: ToolWindow, notification: AITermsNotification) {
    toolWindow.component.add(notification, BoxLayout.Y_AXIS)
  }

  private fun closeExistingNotifications(toolWindow: ToolWindow) {
    val existingNotifications = toolWindow.component.components.filterIsInstance<AITermsNotification>()
    existingNotifications.forEach { it.close() }
  }
}