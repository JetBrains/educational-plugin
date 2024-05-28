package com.jetbrains.edu.learning.checkio.notifications

import com.intellij.notification.NotificationListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.*
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.notification.EduNotification
import com.jetbrains.edu.learning.notification.EduNotificationManager

object CheckiONotifications {

  fun showError(
    project: Project,
    title: @NotificationTitle String,
    subtitle: @NotificationSubtitle String?,
    content: @NotificationContent String,
    listener: NotificationListener? = null
  ) {
    EduNotificationManager.showErrorNotification(
      project,
      title,
      content,
      getCustomization(subtitle, listener)
    )
  }

  fun showWarning(
    project: Project,
    title: @NotificationTitle String,
    subtitle: @NotificationSubtitle String,
    content: @NotificationContent String,
    listener: NotificationListener? = null
  ) {
    EduNotificationManager.showWarningNotification(
      project,
      title,
      content,
      getCustomization(subtitle, listener)
    )
  }

  fun showInfo(
    title: @NotificationTitle String,
    subtitle: @NotificationSubtitle String,
    content: @NotificationContent String,
    listener: NotificationListener? = null
  ) {
    EduNotificationManager.showInfoNotification(
      title = title,
      content = content,
      customization = getCustomization(subtitle, listener)
    )
  }

  private fun getCustomization(
    subtitle: @NotificationSubtitle String?,
    listener: NotificationListener? = null
  ): (EduNotification.() -> Unit) = {
    icon = EducationalCoreIcons.CheckiO
    this.subtitle = subtitle
    if (listener != null) {
      setListener(listener)
    }
  }
}
