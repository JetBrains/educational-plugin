package com.jetbrains.edu.ai.terms.ui

import com.intellij.openapi.util.NlsContexts.NotificationContent
import com.jetbrains.edu.ai.ui.AINotification
import javax.swing.JComponent

class AITermsNotification(
  status: Status,
  @NotificationContent messageText: String,
  parentComponent: JComponent
) : AINotification(status, messageText, parentComponent)