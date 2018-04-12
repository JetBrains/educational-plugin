package com.jetbrains.edu.learning.update

import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.updateSettings.impl.pluginsAdvertisement.PluginsAdvertiser
import com.jetbrains.edu.learning.EduNames
import javax.swing.event.HyperlinkEvent

class UpdateNotification(title: String, content: String) :
  Notification("Plugin Update", title, content, NotificationType.WARNING, UpdateNotificationListener)

private object UpdateNotificationListener : NotificationListener.Adapter() {
  override fun hyperlinkActivated(notification: Notification, e: HyperlinkEvent) {
    PluginsAdvertiser.installAndEnablePlugins(setOf(EduNames.PLUGIN_ID)) {
      notification.expire()
    }
  }
}
