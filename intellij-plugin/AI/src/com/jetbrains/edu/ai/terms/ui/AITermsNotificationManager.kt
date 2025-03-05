package com.jetbrains.edu.ai.terms.ui

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.NotificationContent
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.EditorNotificationPanel
import com.jetbrains.edu.ai.ui.AINotification.ActionLabel
import com.jetbrains.edu.ai.ui.AINotificationManager
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowFactory

@Service(Service.Level.PROJECT)
class AITermsNotificationManager(project: Project) : AINotificationManager<AITermsNotification>(project) {
  override fun getNotifications(toolWindow: ToolWindow): List<AITermsNotification> {
    return toolWindow.component.components.filterIsInstance<AITermsNotification>()
  }

  fun showInfoNotification(message: @NotificationContent String, actionLabel: ActionLabel? = null) {
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TaskToolWindowFactory.STUDY_TOOL_WINDOW) ?: return
    val notification = AITermsNotification(EditorNotificationPanel.Status.Info, message, toolWindow.component)
    if (actionLabel != null) {
      notification.addActionLabel(actionLabel)
    }
    closeExistingNotifications()
    showNotification(toolWindow, notification)
  }

  fun showErrorNotification(message: @NotificationContent String, actionLabel: ActionLabel? = null) {
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TaskToolWindowFactory.STUDY_TOOL_WINDOW) ?: return
    val notification = AITermsNotification(EditorNotificationPanel.Status.Error, message, toolWindow.component)
    if (actionLabel != null) {
      notification.addActionLabel(actionLabel)
    }
    closeExistingNotifications()
    showNotification(toolWindow, notification)
  }

  companion object {
    fun getInstance(project: Project): AITermsNotificationManager = project.service()
  }
}