package com.jetbrains.edu.learning.update

import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType.WARNING
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.installAndEnablePlugin
import com.jetbrains.edu.learning.notification.EduNotificationManager
import javax.swing.event.HyperlinkEvent

fun showUpdateNotification(
  project: Project,
  @NlsContexts.NotificationTitle title: String,
  @NlsContexts.NotificationContent content: String
) {
  EduNotificationManager
    .create(WARNING, title, content)
    .setListener(UpdateNotificationListener)
    .notify(project)
}

private object UpdateNotificationListener : NotificationListener.Adapter() {
  override fun hyperlinkActivated(notification: Notification, e: HyperlinkEvent) {
    installAndEnablePlugin(setOf(PluginId.getId(EduNames.PLUGIN_ID))) {
      notification.expire()
    }
  }
}
