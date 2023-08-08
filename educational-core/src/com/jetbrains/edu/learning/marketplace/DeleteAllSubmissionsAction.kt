package com.jetbrains.edu.learning.marketplace

import com.intellij.CommonBundle
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.jetbrains.edu.coursecreator.CCNotificationUtils.showErrorNotification
import com.jetbrains.edu.coursecreator.CCNotificationUtils.showNotification
import com.jetbrains.edu.coursecreator.CCUtils.showLoginNeededNotification
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.invokeLater
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.runInBackground
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import org.jetbrains.annotations.NonNls

@Suppress("ComponentNotRegistered")
class DeleteAllSubmissionsAction : AnAction(EduCoreBundle.lazyMessage("marketplace.action.delete.all.submissions")) {

  override fun update(e: AnActionEvent) {
    val project = e.project ?: return
    e.presentation.isEnabledAndVisible = false
    val course = StudyTaskManager.getInstance(project).course ?: return
    e.presentation.isEnabledAndVisible = course.isMarketplace
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val account = MarketplaceConnector.getInstance().account ?: return
    val userName = account.userInfo.name

    MarketplaceConnector.getInstance().isLoggedInAsync()
      .thenApply { isLoggedIn ->
        if (isLoggedIn) {
          project.invokeLater {
            if (askActionConfirmation(project, userName)) {
              doDeleteSubmissions(project, userName)
            }
          }
        }
        else {
          showLoginNeededNotification(project, e.presentation.text) { MarketplaceConnector.getInstance().doAuthorize() }
        }
      }
  }

  private fun doDeleteSubmissions(project: Project, userName: String) {
    runInBackground(project, title = EduCoreBundle.getMessage("marketplace.delete.submissions.background.title")) {
      val success = MarketplaceSubmissionsConnector.getInstance().deleteAllSubmissions(userName)
      if (success) {
        SubmissionsManager.getInstance(project).deleteCourseSubmissionsLocally()
        showNotification(
          project,
          EduCoreBundle.message("marketplace.delete.submissions.success.title"),
          EduCoreBundle.message("marketplace.delete.submissions.success.message", userName)
        )
      }
      else {
        showErrorNotification(
          project,
          EduCoreBundle.message("marketplace.delete.submissions.failed.title"),
          EduCoreBundle.message("marketplace.delete.submissions.failed.message", userName)
        )
      }
    }
  }

  private fun askActionConfirmation(project: Project, userName: String): Boolean {
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
        return false
      }
    }
    return true
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

  companion object {
    @NonNls
    const val ACTION_ID = "Educational.Student.DeleteAllSubmissions"
  }
}