package com.jetbrains.edu.learning.taskToolWindow.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.InlineBanner
import com.intellij.util.ui.HTMLEditorKitBuilder
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.marketplace.MarketplaceNotificationUtils
import com.jetbrains.edu.learning.marketplace.SolutionSharingPromptCounter
import com.jetbrains.edu.learning.marketplace.settings.MarketplaceSettings
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import javax.swing.JEditorPane

object SolutionSharingInlineBanners {

  fun promptToEnableSolutionSharing(project: Project) {
    val inlineBanner = SolutionSharingInlineBanner(EditorNotificationPanel.Status.Info).apply {
      setMessage(EduCoreBundle.message("marketplace.solutions.sharing.inline.banner.prompt.action.text"))
      addAction(EduCoreBundle.message("marketplace.solutions.sharing.inline.banner.prompt.description")) {
        MarketplaceSettings.INSTANCE.updateSharingPreference(true, project)
        EduCounterUsageCollector.solutionSharingInviteAction(true)
        removeFromParent()
      }
      setCloseAction {
        EduCounterUsageCollector.solutionSharingInviteAction(false)
      }
    }
    TaskToolWindowView.getInstance(project).addInlineBanner(inlineBanner)
    EduCounterUsageCollector.solutionSharingPromptShown()
    SolutionSharingPromptCounter.update()
  }

  fun showSuccessSolutionSharingEnabling(project: Project) {
    val inlineBanner = SolutionSharingInlineBanner(EditorNotificationPanel.Status.Success).apply {
      // This is a local fix, which will also be available on the platform soon.
      val editorPane = UIUtil.findComponentOfType(this, JEditorPane::class.java)
      editorPane?.editorKit = HTMLEditorKitBuilder().build()
      setMessage(EduCoreBundle.message("marketplace.solutions.sharing.inline.banner.success.description"))
    }
    TaskToolWindowView.getInstance(project).addInlineBanner(inlineBanner)
  }

  fun showFailedToEnableSolutionSharing(project: Project?) {
    if (project == null) {
      MarketplaceNotificationUtils.showFailedToChangeSharingPreferenceNotification()
      return
    }

    val inlineBanner = SolutionSharingInlineBanner(EditorNotificationPanel.Status.Error).apply {
      setMessage(EduCoreBundle.message("marketplace.solutions.sharing.inline.banner.failure.description"))
    }
    TaskToolWindowView.getInstance(project).addInlineBanner(inlineBanner)
  }
}

class SolutionSharingInlineBanner(status: EditorNotificationPanel.Status) : InlineBanner(status)
