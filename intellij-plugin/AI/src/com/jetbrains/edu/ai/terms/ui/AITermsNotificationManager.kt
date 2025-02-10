package com.jetbrains.edu.ai.terms.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.NotificationContent
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.EditorNotificationPanel
import com.jetbrains.edu.ai.ui.AINotification.ActionLabel
import com.jetbrains.edu.ai.ui.AINotificationManager
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowFactory

object AITermsNotificationManager : AINotificationManager<AITermsNotification>() {
  fun showInfoNotification(project: Project, message: @NotificationContent String, actionLabel: ActionLabel? = null) {
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TaskToolWindowFactory.STUDY_TOOL_WINDOW) ?: return
    val notification = AITermsNotification(EditorNotificationPanel.Status.Info, message, toolWindow.component)
    if (actionLabel != null) {
      notification.addActionLabel(actionLabel)
    }
    closeExistingNotifications<AITermsNotification>(toolWindow)
    showNotification(toolWindow, notification)
  }

  fun showErrorNotification(project: Project, message: @NotificationContent String, actionLabel: ActionLabel? = null) {
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TaskToolWindowFactory.STUDY_TOOL_WINDOW) ?: return
    val notification = AITermsNotification(EditorNotificationPanel.Status.Error, message, toolWindow.component)
    if (actionLabel != null) {
      notification.addActionLabel(actionLabel)
    }
    closeExistingNotifications<AITermsNotification>(toolWindow)
    showNotification(toolWindow, notification)
  }
}