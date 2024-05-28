package com.jetbrains.edu.learning.notification

import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

object EduNotificationManager {
  private fun showNotification(
    type: NotificationType,
    project: Project? = null,
    title: String = "",
    content: String,
    customization: (EduNotification.() -> Unit)? = null
  ) {
    EduNotification.create(type, title, content, customization).notify(project)
  }

  fun showInfoNotification(
    project: Project? = null,
    title: String = "",
    content: String,
    customization: (EduNotification.() -> Unit)? = null
  ) {
    showNotification(NotificationType.INFORMATION, project, title, content, customization)
  }

  fun showWarningNotification(
    project: Project? = null,
    title: String = "",
    content: String,
    customization: (EduNotification.() -> Unit)? = null
  ) {
    showNotification(NotificationType.WARNING, project, title, content, customization)
  }

  fun showErrorNotification(
    project: Project? = null,
    title: String = "",
    content: String,
    customization: (EduNotification.() -> Unit)? = null
  ) {
    showNotification(NotificationType.ERROR, project, title, content, customization)
  }
}