package com.jetbrains.edu.coursecreator

import com.intellij.notification.NotificationType.ERROR
import com.intellij.notification.NotificationType.INFORMATION
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.NotificationContent
import com.intellij.openapi.util.NlsContexts.NotificationTitle
import com.jetbrains.edu.learning.notification.EduNotificationManager

object CCNotificationUtils {
  private val LOG = Logger.getInstance(CCNotificationUtils::class.java)

  val showLogAction: AnAction
    get() = ActionManager.getInstance().getAction("ShowLog")

  fun showInfoNotification(
    project: Project,
    @NotificationTitle title: String,
    @NotificationContent message: String = "",
    action: AnAction? = null
  ) {
    EduNotificationManager
      .create(INFORMATION, title, message)
      .apply { action?.let { addAction(it) } }
      .notify(project)
  }

  fun showErrorNotification(
    project: Project,
    @NotificationTitle title: String,
    @NotificationContent message: String? = null,
    action: AnAction? = null
  ) {
    LOG.error(message)
    EduNotificationManager
      .create(ERROR, title, message.orEmpty())
      .apply { action?.let { addAction(it) } }
      .notify(project)
  }
}
