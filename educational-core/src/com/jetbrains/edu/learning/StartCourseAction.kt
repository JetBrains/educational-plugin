package com.jetbrains.edu.learning

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

  abstract val dialog: ImportCourseDialog
  abstract val courseConnector: CourseConnector

  override fun actionPerformed(e: AnActionEvent) {
    doImport()
  }

  private fun doImport() {
    val course = importCourse() ?: return
    JoinCourseDialog(course).show()
  }

  open fun importCourse(): EduCourse? {
    val courseLink = showDialogAndGetCourseLink() ?: return null
    val course = courseConnector.getCourseInfoByLink(courseLink)
    if (course == null) {
      showFailedToAddCourseNotification(courseLink)
      return null
    }
    return course
  }

  private fun showDialogAndGetCourseLink(): String? {
    val inputDialog = dialog
    if (!inputDialog.showAndGet()) {
      return null
    }
    return inputDialog.courseLink()
  }

  protected fun showFailedToAddCourseNotification(courseLink: String) {
    Messages.showErrorDialog(message("error.failed.to.find.course.by.link", platformName, courseLink),
                             message("error.failed.to.find.course.title", platformName))
  }

  protected fun showFailedImportCourseMessage(message: String) = Messages.showErrorDialog(message, message("error.failed.to.import.course"))
}
