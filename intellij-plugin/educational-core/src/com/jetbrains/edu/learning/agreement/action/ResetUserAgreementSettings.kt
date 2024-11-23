package com.jetbrains.edu.learning.agreement.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.learning.agreement.UserAgreementSettings
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.notification.EduNotificationManager

@Suppress("ComponentNotRegistered")
class ResetUserAgreementSettings : DumbAwareAction() {
  override fun actionPerformed(e: AnActionEvent) {
    UserAgreementSettings.getInstance().resetUserAgreementSettings()
    @Suppress("DialogTitleCapitalization")
    EduNotificationManager.showInfoNotification(
      e.project,
      EduCoreBundle.message("action.Educational.Agreement.ResetUserAgreementSettings.notification.title"),
      EduCoreBundle.message("action.Educational.Agreement.ResetUserAgreementSettings.notification.text")
    )
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}