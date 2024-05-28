package com.jetbrains.edu.learning.notification

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.NotificationType.*
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.util.NlsContexts.NotificationContent
import com.intellij.openapi.util.NlsContexts.NotificationTitle

class EduNotification private constructor(
  type: NotificationType,
  @NotificationTitle title: String,
  @NotificationContent content: String
) : Notification(JETBRAINS_ACADEMY_GROUP_ID, title, content, type) {

  fun addAction(action: AnAction?) {
    if (action != null) {
      addAction(action)
    }
  }

  companion object {
    private const val JETBRAINS_ACADEMY_GROUP_ID = "JetBrains Academy"

    internal fun create(
      type: NotificationType,
      title: String,
      content: String,
      customization: (EduNotification.() -> Unit)? = null
    ): Notification = when(type) {
      INFORMATION -> EduNotification(INFORMATION, title, content)
      WARNING -> EduNotification(WARNING, title, content)
      ERROR -> EduNotification(ERROR, title, content)
      IDE_UPDATE -> throw IllegalArgumentException("IDE_UPDATE notification is not supported")
    }.apply { customization?.invoke(this) }
  }
}
