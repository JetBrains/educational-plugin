package com.jetbrains.edu.learning.marketplace.actions

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.NlsActions
import com.jetbrains.edu.learning.marketplace.MarketplaceNotificationUtils
import com.jetbrains.edu.learning.marketplace.userAgreement.UserAgreementSettings
import com.jetbrains.edu.learning.messages.EduCoreBundle
import java.util.function.Supplier

@Suppress("DialogTitleCapitalization", "ComponentNotRegistered")
class ResetUserAgreementSettingsAction(
  title: Supplier<@NlsActions.ActionText String> = EduCoreBundle.lazyMessage("user.agreement.reset")
) : DumbAwareAction(title) {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project

    UserAgreementSettings.getInstance().isDialogShown = false

    Notification(
      MarketplaceNotificationUtils.JETBRAINS_ACADEMY_GROUP_ID,
      EduCoreBundle.message("user.agreement.reset.notification.title"),
      EduCoreBundle.message("user.agreement.reset.notification.text"),
      NotificationType.INFORMATION
    ).notify(project)
  }
}