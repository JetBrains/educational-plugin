package com.jetbrains.edu.learning.marketplace

import com.intellij.CommonBundle
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.jetbrains.edu.coursecreator.CCNotificationUtils.showErrorNotification
import com.jetbrains.edu.coursecreator.CCNotificationUtils.showNotification
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.runInBackground
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import org.jetbrains.annotations.NonNls

@Suppress("ComponentNotRegistered")
class DeleteAllSubmissionsAction : AnAction(EduCoreBundle.lazyMessage("marketplace.action.delete.all.submissions")) {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return

    val account = MarketplaceConnector.getInstance().account ?: return
    val userName = account.userInfo.name

    if (!isUnitTestMode) {
      val result = Messages.showYesNoDialog(
        project,
        EduCoreBundle.getMessage("marketplace.delete.submissions.dialog.text", userName),
        EduCoreBundle.getMessage("marketplace.delete.submissions.dialog.title"),
        EduCoreBundle.getMessage("marketplace.delete.submissions.dialog.yes.text"),
        CommonBundle.getCancelButtonText(),
        null
      )
      if (result != Messages.YES) {
        return
      }
    }

    runInBackground(project, title = EduCoreBundle.getMessage("marketplace.delete.submissions.background.title")) {
      val success = MarketplaceSubmissionsConnector.getInstance().deleteAllSubmissions(userName)
      if (success) {
        SubmissionsManager.getInstance(project).deleteCourseSubmissionsLocally()
        showNotification(project, EduCoreBundle.message("marketplace.delete.submissions.success.title"),
                         EduCoreBundle.message("marketplace.delete.submissions.success.message", userName))
      }
      else {
        showErrorNotification(project, EduCoreBundle.message("marketplace.delete.submissions.failed.title"),
                              EduCoreBundle.message("marketplace.delete.submissions.failed.message", userName))
      }
    }
  }

  override fun update(e: AnActionEvent) {
    val project = e.project ?: return
    e.presentation.isEnabledAndVisible = EduUtils.isEduProject(project) && MarketplaceConnector.getInstance().isLoggedIn()
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

  companion object {
    @NonNls
    const val ACTION_ID = "Educational.Student.DeleteAllSubmissions"
  }
}