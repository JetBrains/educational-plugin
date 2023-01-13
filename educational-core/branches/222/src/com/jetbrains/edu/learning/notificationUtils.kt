package com.jetbrains.edu.learning

import com.intellij.notification.Notification

fun Notification.configureDoNotAsk(id: String, displayName: String) {
  configureDoNotAskOption(id, displayName)
}