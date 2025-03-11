package com.jetbrains.edu.ai.ui

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.NotificationContent
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.components.panels.NonOpaquePanel
import com.jetbrains.edu.ai.ui.AINotification.ActionLabel
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowFactory
import java.awt.BorderLayout
import javax.swing.BoxLayout

@Service(Service.Level.PROJECT)
class AINotificationManager(project: Project) {
  private val notificationPanel = AINotificationsPanel()

  init {
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TaskToolWindowFactory.STUDY_TOOL_WINDOW)
    toolWindow?.component?.add(notificationPanel, BorderLayout.NORTH)
  }

  fun closeExistingTermsNotifications() = closeExistingNotifications(AINotification.TERMS_NOTIFICATION_ID)
  fun closeExistingTranslationNotifications() = closeExistingNotifications(AINotification.TRANSLATION_NOTIFICATION_ID)

  fun showInfoTranslationNotification(message: @NotificationContent String, actionLabel: ActionLabel? = null) {
    return showNotification(AINotification.TRANSLATION_NOTIFICATION_ID, EditorNotificationPanel.Status.Info, message, actionLabel)
  }

  fun showErrorTranslationNotification(message: @NotificationContent String, actionLabel: ActionLabel? = null) {
    return showNotification(AINotification.TRANSLATION_NOTIFICATION_ID, EditorNotificationPanel.Status.Error, message, actionLabel)
  }

  fun showInfoTermsNotification(message: @NotificationContent String, actionLabel: ActionLabel? = null) {
    return showNotification(AINotification.TERMS_NOTIFICATION_ID, EditorNotificationPanel.Status.Info, message, actionLabel)
  }

  fun showErrorTermsNotification(message: @NotificationContent String, actionLabel: ActionLabel? = null) {
    return showNotification(AINotification.TERMS_NOTIFICATION_ID, EditorNotificationPanel.Status.Error, message, actionLabel)
  }

  private fun showNotification(
    notificationId: String,
    status: EditorNotificationPanel.Status,
    message: @NotificationContent String,
    actionLabel: ActionLabel? = null
  ) {
    val notification = AINotification(notificationId, status, message, notificationPanel)
    actionLabel?.let { notification.addActionLabel(it) }
    closeExistingNotifications(notificationId)
    showNotification(notification)
  }

  private fun closeExistingNotifications(notificationId: String) {
    val existingNotifications = notificationPanel
      .components
      .filterIsInstance<AINotification>()
      .filter { it.id == notificationId }
    existingNotifications.forEach { it.close() }
  }

  private fun showNotification(notification: AINotification) {
    notificationPanel.add(notification)
  }

  private class AINotificationsPanel : NonOpaquePanel() {
    init {
      layout = BoxLayout(this, BoxLayout.Y_AXIS)
    }
  }

  companion object {
    fun getInstance(project: Project): AINotificationManager = project.service()
  }
}