package com.jetbrains.edu.learning.notification

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.util.NlsContexts.NotificationContent
import com.intellij.openapi.util.NlsContexts.NotificationTitle

sealed class EduNotification(@NotificationTitle title: String, @NotificationContent content: String, type: NotificationType) :
  Notification(JETBRAINS_ACADEMY_GROUP_ID, title, content, type) {

  companion object {
    private const val JETBRAINS_ACADEMY_GROUP_ID = "JetBrains Academy"
  }
}

class EduInformationNotification(title: String = "", content: String) : EduNotification(title, content, NotificationType.INFORMATION)
class EduWarningNotification(title: String = "", content: String) : EduNotification(title, content, NotificationType.WARNING)
class EduErrorNotification(title: String = "", content: String) : EduNotification(title, content, NotificationType.ERROR)