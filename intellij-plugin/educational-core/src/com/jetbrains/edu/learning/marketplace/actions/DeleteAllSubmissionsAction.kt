package com.jetbrains.edu.learning.marketplace.actions

import com.intellij.CommonBundle
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.JBAccountInfoService
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.marketplace.MarketplaceNotificationUtils.showLoginNeededNotification
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.runInBackground
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import org.jetbrains.annotations.NonNls

class DeleteAllSubmissionsAction : DumbAwareAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project

    MarketplaceConnector.getInstance().isLoggedInAsync().thenApply { isLoggedIn ->
      if (!isLoggedIn) {
        showLoginNeededNotification(project, e.presentation.text) { MarketplaceConnector.getInstance().doAuthorize() }
        return@thenApply
      }
      ApplicationManager.getApplication().invokeLater {
        val loginName = JBAccountInfoService.getInstance()?.userData?.loginName
        if (askActionConfirmation(project, loginName)) {
          doDeleteSubmissions(project, loginName)
        }
      }
    }
  }

  private fun doDeleteSubmissions(project: Project?, loginName: String?) {
    runInBackground(project, title = EduCoreBundle.message("marketplace.delete.submissions.background.title")) {
      val deleteLocalSubmissions = MarketplaceSubmissionsConnector.getInstance().deleteAllSubmissions(project, loginName)
      if (deleteLocalSubmissions && project != null) {
        SubmissionsManager.getInstance(project).deleteCourseSubmissionsLocally()
      }
    }
  }

  private fun askActionConfirmation(project: Project?, loginName: String?): Boolean {
    if (isUnitTestMode) return true

    val dialogText = if (loginName != null) {
      EduCoreBundle.message("marketplace.delete.submissions.dialog.for.user.text", loginName)
    }
    else {
      EduCoreBundle.message("marketplace.delete.submissions.dialog.text")
    }
    val result = Messages.showYesNoDialog(
      project,
      dialogText,
      EduCoreBundle.message("marketplace.delete.submissions.dialog.title"),
      EduCoreBundle.message("marketplace.delete.submissions.dialog.yes.text"),
      CommonBundle.getCancelButtonText(),
      null
    )

    return result == Messages.YES
  }

  companion object {
    @NonNls
    const val ACTION_ID = "Educational.Student.DeleteAllSubmissions"
  }
}