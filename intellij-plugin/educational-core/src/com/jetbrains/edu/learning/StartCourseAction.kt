package com.jetbrains.edu.learning

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.messages.EduCoreBundle.message
import com.jetbrains.edu.learning.newproject.ui.JoinCourseDialog
import com.jetbrains.edu.learning.stepik.course.CourseConnector
import com.jetbrains.edu.learning.stepik.course.ImportCourseDialog

abstract class StartCourseAction(
  private val platformName: String
) : DumbAwareAction(EduCoreBundle.lazyMessage("action.start.course", platformName)) {

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
    val course = courseConnector().getCourseInfoByLink(courseLink)
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

  private fun showFailedToAddCourseNotification(courseLink: String) {
    Messages.showErrorDialog(
      message("error.failed.to.find.course.by.link", platformName, courseLink),
      message("error.failed.to.find.course.title", platformName)
    )
  }

  /**
   * Returns instance of [ImportCourseDialog] to show text field and get a course link typed by user
   */
  protected abstract fun createImportCourseDialog(): ImportCourseDialog
  protected abstract fun courseConnector(): CourseConnector
}
