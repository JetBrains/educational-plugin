package com.jetbrains.edu.learning

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.platform.ide.progress.ModalTaskOwner
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.JoinCourseDialog
import com.jetbrains.edu.learning.marketplace.api.EduCourseConnector
import com.jetbrains.edu.learning.stepik.course.ImportCourseDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

abstract class StartCourseAction : DumbAwareAction() {
  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = !RemoteEnvHelper.isRemoteDevServer()
  }

  override fun actionPerformed(e: AnActionEvent) {
    doImport()
  }

  private fun doImport() {
    val course = importCourse() ?: return
    JoinCourseDialog(course).show()
  }

  private fun importCourse(): EduCourse? {
    val courseLink = showDialogAndGetCourseLink() ?: return null
    val course = runWithModalProgressBlocking(owner = ModalTaskOwner.guess(), title = EduCoreBundle.message("progress.loading.course")) {
      withContext(Dispatchers.IO) {
        courseConnector().getCourseInfoByLink(courseLink)
      }
    }
    if (course == null) {
      showFailedToAddCourseNotification(courseLink)
      return null
    }
    return course
  }

  private fun showDialogAndGetCourseLink(): String? {
    val inputDialog = createImportCourseDialog()
    if (!inputDialog.showAndGet()) {
      return null
    }
    return inputDialog.courseLink()
  }

  abstract fun showFailedToAddCourseNotification(courseLink: String)

  /**
   * Returns instance of [ImportCourseDialog] to show text field and get a course link typed by user
   */
  protected abstract fun createImportCourseDialog(): ImportCourseDialog
  protected abstract fun courseConnector(): EduCourseConnector
}
