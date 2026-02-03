package com.jetbrains.edu.coursecreator

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.util.NlsContexts.NotificationContent
import com.intellij.openapi.util.NlsContexts.NotificationTitle
import com.jetbrains.edu.learning.notification.EduNotificationManager

/**
 * Represents an error occurred during course uploading related to the course vendor.
 *
 * It's supposed that the error will be transformed to error notification and shown to users
 */
data class VendorError(@NotificationContent val message: String, private val notificationAction: AnAction? = null) {
  fun notification(@NotificationTitle title: String): Notification {
    val notification = EduNotificationManager.create(NotificationType.ERROR, title, message)
    if (notificationAction != null) {
      notification.addAction(notificationAction)
    }
    return notification
  }
}
