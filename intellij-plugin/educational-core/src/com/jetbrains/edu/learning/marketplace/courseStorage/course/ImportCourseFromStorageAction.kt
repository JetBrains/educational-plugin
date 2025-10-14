package com.jetbrains.edu.learning.marketplace.courseStorage.course

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.jetbrains.edu.learning.StartCourseAction
import com.jetbrains.edu.learning.marketplace.courseStorage.api.CourseStorageConnector
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.marketplace.api.EduCourseConnector
import com.jetbrains.edu.learning.stepik.course.ImportCourseDialog

class ImportCourseFromStorageAction : StartCourseAction() {
  override fun courseConnector(): EduCourseConnector = CourseStorageConnector.getInstance()
  override fun createImportCourseDialog(): ImportCourseDialog = ImportCourseFromStorageDialog(courseConnector())

  override fun update(e: AnActionEvent) {
    super.update(e)
    if (!e.presentation.isEnabledAndVisible) return
    e.presentation.isEnabledAndVisible = CourseStorageConnector.getInstance().isLoggedIn()
  }

  override fun showFailedToAddCourseNotification(courseLink: String) {
    Messages.showErrorDialog(
      EduCoreBundle.message("course.storage.error.failed.to.find.course.by.link", courseLink),
      EduCoreBundle.message("course.storage.error.failed.to.find.course.title")
    )
  }
}