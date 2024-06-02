package com.jetbrains.edu.learning.marketplace.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.NlsActions
import com.jetbrains.edu.learning.marketplace.userAgreement.UserAgreementSettings
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.notification.EduNotificationManager
import java.util.function.Supplier

@Suppress("ComponentNotRegistered")
class ResetUserAgreementSettingsAction(
  title: Supplier<@NlsActions.ActionText String> = EduCoreBundle.lazyMessage("user.agreement.reset")
) : DumbAwareAction(title) {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project

    UserAgreementSettings.getInstance().isDialogShown = false

    EduNotificationManager.showInfoNotification(
      project,
      @Suppress("DialogTitleCapitalization") EduCoreBundle.message("user.agreement.reset.notification.title"),
      EduCoreBundle.message("user.agreement.reset.notification.text")
    )
  }
}