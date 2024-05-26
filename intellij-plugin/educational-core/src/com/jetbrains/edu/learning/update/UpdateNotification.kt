package com.jetbrains.edu.learning.update

import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.util.NlsContexts
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.installAndEnablePlugin
import com.jetbrains.edu.learning.notification.EduWarningNotification
import javax.swing.event.HyperlinkEvent

class UpdateNotification @Suppress("UnstableApiUsage") constructor(
  @NlsContexts.NotificationTitle title: String,
  @NlsContexts.NotificationContent content: String
) : EduWarningNotification(title, content) {
  init {
    setListener(UpdateNotificationListener)
  }
}

private object UpdateNotificationListener : NotificationListener.Adapter() {
  override fun hyperlinkActivated(notification: Notification, e: HyperlinkEvent) {
    installAndEnablePlugin(setOf(PluginId.getId(EduNames.PLUGIN_ID))) {
      notification.expire()
    }
  }
}
