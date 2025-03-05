package com.jetbrains.edu.ai.translation.ui

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
class AITranslationNotificationManager(project: Project) : AINotificationManager<AITranslationNotification>(project) {
  override fun getNotifications(toolWindow: ToolWindow): List<AITranslationNotification> {
    return toolWindow.component.components.filterIsInstance<AITranslationNotification>()
  }

  fun showInfoNotification(message: @NotificationContent String, actionLabel: ActionLabel? = null) {
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TaskToolWindowFactory.STUDY_TOOL_WINDOW) ?: return
    val notification = AITranslationNotification(EditorNotificationPanel.Status.Info, message, toolWindow.component)
    if (actionLabel != null) {
      notification.addActionLabel(actionLabel)
    }
    closeExistingNotifications()
  }

  fun showErrorNotification(message: @NotificationContent String, actionLabel: ActionLabel? = null) {
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TaskToolWindowFactory.STUDY_TOOL_WINDOW) ?: return
    val notification = AITranslationNotification(EditorNotificationPanel.Status.Error, message, toolWindow.component)
    if (actionLabel != null) {
      notification.addActionLabel(actionLabel)
    }
    closeExistingNotifications()
    showNotification(toolWindow, notification)
  }

  companion object {
    fun getInstance(project: Project): AITranslationNotificationManager = project.service()
  }
}