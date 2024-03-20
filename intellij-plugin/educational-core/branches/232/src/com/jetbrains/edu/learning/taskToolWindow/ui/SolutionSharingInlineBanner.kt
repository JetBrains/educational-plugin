package com.jetbrains.edu.learning.taskToolWindow.ui

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.marketplace.MarketplaceNotificationUtils
import com.jetbrains.edu.learning.marketplace.SolutionSharingPromptCounter
import com.jetbrains.edu.learning.marketplace.settings.MarketplaceSettings
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import java.awt.BorderLayout
import javax.swing.JPanel

object SolutionSharingInlineBanners {

  fun promptToEnableSolutionSharing(project: Project) {
    Notification(
      MarketplaceNotificationUtils.JETBRAINS_ACADEMY_GROUP_ID,
      EduCoreBundle.message("marketplace.solutions.sharing.inline.banner.prompt.description"),
      EduCoreBundle.message("marketplace.solutions.sharing.inline.banner.prompt.action.text"),
      NotificationType.INFORMATION
    ).apply {
      addAction(NotificationAction.createSimpleExpiring(EduCoreBundle.message("marketplace.solutions.sharing.inline.banner.prompt.description")) {
        MarketplaceSettings.INSTANCE.updateSharingPreference(true, project)
        EduCounterUsageCollector.solutionSharingInviteAction(true)
        hideBalloon()
      })
      whenExpired {
        EduCounterUsageCollector.solutionSharingInviteAction(false)
      }
      isSuggestionType = true
      notify(project)
    }
    EduCounterUsageCollector.solutionSharingPromptShown()
    SolutionSharingPromptCounter.update()
  }

  fun showSuccessSolutionSharingEnabling(project: Project) = Notification(
    MarketplaceNotificationUtils.JETBRAINS_ACADEMY_GROUP_ID,
    EduCoreBundle.message("marketplace.solutions.sharing.inline.banner.prompt.description"),
    EduCoreBundle.message("marketplace.solutions.sharing.inline.banner.success.description"),
    NotificationType.INFORMATION
  ).notify(project)

  @Suppress("Unused_Parameter")
  fun showFailedToEnableSolutionSharing(project: Project?) {
    MarketplaceNotificationUtils.showFailedToChangeSharingPreferenceNotification()
  }
}

class SolutionSharingInlineBanner : JPanel(BorderLayout())
