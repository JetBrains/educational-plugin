package com.jetbrains.edu.learning.notification

import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

object EduNotificationManager {
  fun showNotification(project: Project? = null, title: String = "", content: String, type: NotificationType) {
    EduNotification.create(title, content, type).notify(project)
  }

  fun showInfoNotification(project: Project? = null, title: String = "", content: String) {
    showNotification(project, title, content, NotificationType.INFORMATION)
  }

  fun showWarningNotification(project: Project? = null, title: String = "", content: String) {
    showNotification(project, title, content, NotificationType.WARNING)
  }

  fun showErrorNotification(project: Project? = null, title: String = "", content: String) {
    showNotification(project, title, content, NotificationType.ERROR)
  }
}