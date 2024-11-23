package com.jetbrains.edu.learning.taskToolWindow.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.InlineBanner
import com.intellij.util.ui.HTMLEditorKitBuilder
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.agreement.UserAgreementDialog
import com.jetbrains.edu.learning.agreement.UserAgreementSettings
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.invokeLater
import com.jetbrains.edu.learning.marketplace.SolutionSharingPromptCounter
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import java.util.concurrent.CompletableFuture
import javax.swing.JEditorPane

object SolutionSharingInlineBanners {

  fun promptToEnableSolutionSharing(project: Project, task: Task) {
    if (!SolutionSharingPromptCounter.shouldPrompt() || !task.supportSubmissions) return

    val inlineBanner = InlineBanner(EditorNotificationPanel.Status.Info).apply {
      setMessage(EduCoreBundle.message("marketplace.solutions.sharing.inline.banner.prompt.action.text"))
      addAction(EduCoreBundle.message("marketplace.solutions.sharing.inline.banner.prompt.description")) {
        project.invokeLater {
          if (UserAgreementSettings.getInstance().solutionSharing || UserAgreementDialog.showEnableSubmissionsDialog(project)) {
            CompletableFuture.runAsync {
              UserAgreementSettings.getInstance().setSolutionSharing()
              showSuccessSolutionSharingEnabling(project)
              EduCounterUsageCollector.solutionSharingInviteAction(true)
            }
            removeFromParent()
          }
        }
      }
      setCloseAction {
        EduCounterUsageCollector.solutionSharingInviteAction(false)
      }
    }
    TaskToolWindowView.getInstance(project).addInlineBanner(inlineBanner)
    EduCounterUsageCollector.solutionSharingPromptShown()
    SolutionSharingPromptCounter.update()
  }

  private fun showSuccessSolutionSharingEnabling(project: Project) {
    val inlineBanner = InlineBanner(EditorNotificationPanel.Status.Success).apply {
      // This is a local fix, which will also be available on the platform soon.
      val editorPane = UIUtil.findComponentOfType(this, JEditorPane::class.java)
      editorPane?.editorKit = HTMLEditorKitBuilder().build()
      setMessage(EduCoreBundle.message("marketplace.solutions.sharing.inline.banner.success.description"))
    }
    TaskToolWindowView.getInstance(project).addInlineBanner(inlineBanner)
  }
}
