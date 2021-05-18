package com.jetbrains.edu.learning.checkio.notifications

import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.util.NlsContexts.*
import icons.EducationalCoreIcons

@Suppress("UnstableApiUsage")
object CheckiONotifications {

  @JvmOverloads
  @JvmStatic
  fun error(
    title: @NotificationTitle String,
    subtitle: @NotificationSubtitle String,
    content: @NotificationContent String,
    listener: NotificationListener? = null
  ): Notification {
    return notification(title, subtitle, content, NotificationType.ERROR, listener)
  }

  @JvmOverloads
  @JvmStatic
  fun warn(
    title: @NotificationTitle String,
    subtitle: @NotificationSubtitle String,
    content: @NotificationContent String,
    listener: NotificationListener? = null
  ): Notification {
    return notification(title, subtitle, content, NotificationType.WARNING, listener)
  }

  @JvmOverloads
  @JvmStatic
  fun info(
    title: @NotificationTitle String,
    subtitle: @NotificationSubtitle String,
    content: @NotificationContent String,
    listener: NotificationListener? = null
  ): Notification {
    return notification(title, subtitle, content, NotificationType.INFORMATION, listener)
  }

  private fun notification(
    title: @NotificationTitle String,
    subtitle: @NotificationSubtitle String,
    content: @NotificationContent String,
    type: NotificationType,
    listener: NotificationListener?
  ): Notification {
    return Notification("EduTools", EducationalCoreIcons.CheckiO, title, subtitle, content, type, listener)
  }
}
