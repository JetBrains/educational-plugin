package com.jetbrains.edu.learning.marketplace.deleteSubmissions

import com.intellij.CommonBundle
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.JBAccountInfoService
import com.jetbrains.edu.learning.course
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
    val presentationText = e.presentation.text
    val marketplaceConnector = MarketplaceConnector.getInstance()
    if (!isUnitTestMode) {
      marketplaceConnector.isLoggedInAsync().thenApply { isLoggedIn ->
        doAction(isLoggedIn, project, presentationText)
      }
    }
    else {
      doAction(marketplaceConnector.isLoggedIn(), project, presentationText)
    }
  }

  private fun doAction(isLoggedIn: Boolean, project: Project?, presentationText: String) {
    if (!isLoggedIn) {
      showLoginNeededNotification(project, presentationText) { MarketplaceConnector.getInstance().doAuthorize() }
      return
    }

    ApplicationManager.getApplication().invokeLater {
      val loginName = JBAccountInfoService.getInstance()?.userData?.loginName
      if (project != null) {
        advancedDeleteSubmissions(project, loginName)
        return@invokeLater
      }
      defaultDeleteSubmissions(loginName)
    }
  }

  private fun advancedDeleteSubmissions(project: Project, loginName: String?) {
    val dialogResult = AdvancedSubmissionsDeleteDialog.showConfirmationDialog(project)
    when (dialogResult) {
      AdvancedSubmissionsDeleteDialog.CANCEL -> return
      AdvancedSubmissionsDeleteDialog.COURSE -> doDeleteCourseSubmissions(project, loginName)
      AdvancedSubmissionsDeleteDialog.ALL -> doDeleteSubmissions(project, loginName)
    }
  }

  private fun defaultDeleteSubmissions(loginName: String?) {
    if (askActionConfirmation(loginName)) {
      doDeleteSubmissions(loginName = loginName)
    }
  }

  private fun doDeleteCourseSubmissions(project: Project, loginName: String?) {
    val courseId = project.course?.id ?: return
    runInBackground(project, title = EduCoreBundle.message("marketplace.delete.course.submissions.background.title")) {
      val deleteLocalSubmissions = MarketplaceSubmissionsConnector.getInstance().deleteAllSubmissions(project, courseId, loginName)
      if (deleteLocalSubmissions) {
        SubmissionsManager.getInstance(project).deleteCourseSubmissionsLocally()
      }
    }
  }

  private fun doDeleteSubmissions(project: Project? = null, loginName: String?) = runInBackground(project, title = EduCoreBundle.message("marketplace.delete.submissions.background.title")) {
    val deleteLocalSubmissions = MarketplaceSubmissionsConnector.getInstance().deleteAllSubmissions(project, loginName = loginName)
    if (deleteLocalSubmissions && project != null) {
      SubmissionsManager.getInstance(project).deleteCourseSubmissionsLocally()
    }
  }

  private fun askActionConfirmation(loginName: String?): Boolean {
    val dialogText = if (loginName != null) {
      EduCoreBundle.message("marketplace.delete.submissions.dialog.for.user.text", loginName)
    }
    else {
      EduCoreBundle.message("marketplace.delete.submissions.dialog.text")
    }
    val result = Messages.showYesNoDialog(
      null,
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
