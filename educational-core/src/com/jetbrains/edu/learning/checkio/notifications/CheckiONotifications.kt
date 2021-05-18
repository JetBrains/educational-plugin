package com.jetbrains.edu.learning.checkio.notifications

import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import icons.EducationalCoreIcons
import org.jetbrains.annotations.Nls

object CheckiONotifications {

  @JvmOverloads
  @JvmStatic
  fun error(
    title: @Nls(capitalization = Nls.Capitalization.Sentence) String?,
    subtitle: @Nls(capitalization = Nls.Capitalization.Sentence) String?,
    content: @Nls(capitalization = Nls.Capitalization.Sentence) String?,
    listener: NotificationListener? = null
  ): Notification {
    return notification(title, subtitle, content, NotificationType.ERROR, listener)
  }

  @JvmOverloads
  @JvmStatic
  fun warn(
    title: @Nls(capitalization = Nls.Capitalization.Sentence) String?,
    subtitle: @Nls(capitalization = Nls.Capitalization.Sentence) String?,
    content: @Nls(capitalization = Nls.Capitalization.Sentence) String?,
    listener: NotificationListener? = null
  ): Notification {
    return notification(title, subtitle, content, NotificationType.WARNING, listener)
  }

  @JvmOverloads
  @JvmStatic
  fun info(
    title: @Nls(capitalization = Nls.Capitalization.Sentence) String?,
    subtitle: @Nls(capitalization = Nls.Capitalization.Sentence) String?,
    content: @Nls(capitalization = Nls.Capitalization.Sentence) String?,
    listener: NotificationListener? = null
  ): Notification {
    return notification(title, subtitle, content, NotificationType.INFORMATION, listener)
  }

  private fun notification(
    title: @Nls(capitalization = Nls.Capitalization.Sentence) String?,
    subtitle: @Nls(capitalization = Nls.Capitalization.Sentence) String?,
    content: @Nls(capitalization = Nls.Capitalization.Sentence) String?,
    type: NotificationType,
    listener: NotificationListener?
  ): Notification {
    return Notification("EduTools", EducationalCoreIcons.CheckiO, title, subtitle, content, type, listener)
  }
}
