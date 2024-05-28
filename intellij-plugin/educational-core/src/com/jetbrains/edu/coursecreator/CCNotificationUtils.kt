package com.jetbrains.edu.coursecreator

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
    EduNotificationManager.showInfoNotification(project, title, message) {
      addAction(action)
    }
  }

  fun showErrorNotification(
    project: Project,
    @NotificationTitle title: String,
    @NotificationContent message: String? = null,
    action: AnAction? = null
  ) {
    LOG.error(message)
    EduNotificationManager.showErrorNotification(project, title, message.orEmpty()) {
      addAction(action)
    }
  }
}
