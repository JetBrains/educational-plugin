package com.jetbrains.edu.ai.translation.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.NotificationContent
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.EditorNotificationPanel
import com.jetbrains.edu.ai.translation.ui.AITranslationNotification.ActionLabel
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowFactory
import java.awt.BorderLayout

object AITranslationNotificationManager {
  fun showInfoNotification(project: Project, message: @NotificationContent String, actionLabel: ActionLabel? = null) {
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TaskToolWindowFactory.STUDY_TOOL_WINDOW) ?: return
    val notification = AITranslationNotification(EditorNotificationPanel.Status.Info, message, toolWindow.component)
    if (actionLabel != null) {
      notification.addActionLabel(actionLabel)
    }
    closeExistingNotifications(toolWindow)
    showNotification(toolWindow, notification)
  }

  fun showErrorNotification(project: Project, message: @NotificationContent String, actionLabel: ActionLabel? = null) {
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TaskToolWindowFactory.STUDY_TOOL_WINDOW) ?: return
    val notification = AITranslationNotification(EditorNotificationPanel.Status.Error, message, toolWindow.component)
    if (actionLabel != null) {
      notification.addActionLabel(actionLabel)
    }
    closeExistingNotifications(toolWindow)
    showNotification(toolWindow, notification)
  }

  private fun showNotification(toolWindow: ToolWindow, notification: AITranslationNotification) {
    toolWindow.component.add(notification, BorderLayout.NORTH)
  }

  private fun closeExistingNotifications(toolWindow: ToolWindow) {
    val existingNotifications = toolWindow.component.components.filterIsInstance<AITranslationNotification>()
    existingNotifications.forEach { it.close() }
  }
}