package com.jetbrains.edu.learning.marketplace.actions

import com.intellij.CommonBundle
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.JBAccountInfoService
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

class DeleteAllSubmissionsAction : AnAction(EduCoreBundle.lazyMessage("marketplace.action.delete.all.submissions")) {

  override fun update(e: AnActionEvent) {
    val project = e.project ?: return
    e.presentation.isEnabledAndVisible = false
    val course = StudyTaskManager.getInstance(project).course ?: return
    e.presentation.isEnabledAndVisible = course.isMarketplace
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return

    MarketplaceConnector.getInstance().isLoggedInAsync()
      .thenApply { isLoggedIn ->
        if (isLoggedIn) {
          val loginName = JBAccountInfoService.getInstance()?.userData?.loginName
          project.invokeLater {
            if (askActionConfirmation(project, loginName)) {
              doDeleteSubmissions(project, loginName)
            }
          }
        }
        else {
          showLoginNeededNotification(project, e.presentation.text) { MarketplaceConnector.getInstance().doAuthorize() }
        }
      }
  }

  private fun doDeleteSubmissions(project: Project, loginName: String?) {
    runInBackground(project, title = EduCoreBundle.getMessage("marketplace.delete.submissions.background.title")) {
      val deleteLocalSubmissions = MarketplaceSubmissionsConnector.getInstance().deleteAllSubmissions(project, loginName)
      if (deleteLocalSubmissions) {
        SubmissionsManager.getInstance(project).deleteCourseSubmissionsLocally()
      }
    }
  }

  private fun askActionConfirmation(project: Project, loginName: String?): Boolean {
    if (!isUnitTestMode) {
      val result = Messages.showYesNoDialog(
        project,
        EduCoreBundle.getMessage("marketplace.delete.submissions.dialog.text", loginName),
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