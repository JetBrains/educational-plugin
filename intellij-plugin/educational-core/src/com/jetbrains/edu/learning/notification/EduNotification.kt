package com.jetbrains.edu.learning.notification

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.util.NlsContexts.NotificationContent
import com.intellij.openapi.util.NlsContexts.NotificationTitle

sealed class EduNotification(@NotificationTitle title: String, @NotificationContent content: String, type: NotificationType) :
  Notification(JETBRAINS_ACADEMY_GROUP_ID, title, content, type) {

  companion object {
    private const val JETBRAINS_ACADEMY_GROUP_ID = "JetBrains Academy"

    fun create(title: String, content: String, type: NotificationType): Notification {
      return when(type) {
        NotificationType.INFORMATION -> EduInformationNotification(title, content)
        NotificationType.WARNING -> EduWarningNotification(title, content)
        NotificationType.ERROR -> EduErrorNotification(title, content)
        NotificationType.IDE_UPDATE -> throw IllegalArgumentException("IDE_UPDATE notification is not supported")
      }
    }
  }
}

open class EduInformationNotification(title: String = "", content: String) : EduNotification(title, content, NotificationType.INFORMATION)
open class EduWarningNotification(title: String = "", content: String) : EduNotification(title, content, NotificationType.WARNING)
open class EduErrorNotification(title: String = "", content: String) : EduNotification(title, content, NotificationType.ERROR)