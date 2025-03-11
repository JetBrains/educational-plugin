package com.jetbrains.edu.ai.ui

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.NotificationContent
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.components.panels.NonOpaquePanel
import com.jetbrains.edu.ai.terms.ui.AITermsNotification
import com.jetbrains.edu.ai.translation.ui.AITranslationNotification
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

  fun closeExistingTermsNotifications() = closeExistingNotifications(AITermsNotification::class.java)
  fun closeExistingTranslationNotifications() = closeExistingNotifications(AITranslationNotification::class.java)

  fun showInfoTranslationNotification(message: @NotificationContent String, actionLabel: ActionLabel? = null) {
    return showNotification(AITranslationNotification::class.java, EditorNotificationPanel.Status.Info, message, actionLabel)
  }

  fun showErrorTranslationNotification(message: @NotificationContent String, actionLabel: ActionLabel? = null) {
    return showNotification(AITranslationNotification::class.java, EditorNotificationPanel.Status.Error, message, actionLabel)
  }

  fun showInfoTermsNotification(message: @NotificationContent String, actionLabel: ActionLabel? = null) {
    return showNotification(AITermsNotification::class.java, EditorNotificationPanel.Status.Info, message, actionLabel)
  }

  fun showErrorTermsNotification(message: @NotificationContent String, actionLabel: ActionLabel? = null) {
    return showNotification(AITermsNotification::class.java, EditorNotificationPanel.Status.Error, message, actionLabel)
  }

  private fun <T : AINotification> showNotification(
    notificationType: Class<T>,
    status: EditorNotificationPanel.Status,
    message: @NotificationContent String,
    actionLabel: ActionLabel? = null
  ) {
    val notification = when (notificationType) {
      AITranslationNotification::class.java -> AITranslationNotification(status, message, notificationPanel)
      AITermsNotification::class.java -> AITermsNotification(status, message, notificationPanel)
      else -> {
        LOG.error("Unknown notification type: $notificationType")
        return
      }
    }

    actionLabel?.let { notification.addActionLabel(it) }
    closeExistingNotifications(notificationType)
    showNotification(notification)
  }

  private fun <T : AINotification> closeExistingNotifications(notificationType: Class<T>) {
    val existingNotifications = notificationPanel.components.filterIsInstance(notificationType)
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
    private val LOG = Logger.getInstance(AINotificationManager::class.java)

    fun getInstance(project: Project): AINotificationManager = project.service()
  }
}