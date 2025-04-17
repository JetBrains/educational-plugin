package com.jetbrains.edu.learning.notification

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.NotificationType.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.NotificationContent
import com.intellij.openapi.util.NlsContexts.NotificationTitle

object EduNotificationManager {
  const val JETBRAINS_ACADEMY_GROUP_ID = "JetBrains Academy"

  @Suppress("EduNotificationConversion")
  fun create(type: NotificationType, @NotificationTitle title: String = "", @NotificationContent content: String): Notification =
    when (type) {
      INFORMATION -> Notification(JETBRAINS_ACADEMY_GROUP_ID, title, content, INFORMATION)
      WARNING -> Notification(JETBRAINS_ACADEMY_GROUP_ID, title, content, WARNING)
      ERROR -> Notification(JETBRAINS_ACADEMY_GROUP_ID, title, content, ERROR)
      IDE_UPDATE -> throw IllegalArgumentException("IDE_UPDATE notification is not supported")
    }

  fun showInfoNotification(project: Project? = null, @NotificationTitle title: String = "", @NotificationContent content: String) {
    create(INFORMATION, title, content).notify(project)
  }

  fun showWarningNotification(project: Project? = null, @NotificationTitle title: String = "", @NotificationContent content: String) {
    create(WARNING, title, content).notify(project)
  }

  fun showErrorNotification(project: Project? = null, @NotificationTitle title: String = "", @NotificationContent content: String) {
    create(ERROR, title, content).notify(project)
  }
}