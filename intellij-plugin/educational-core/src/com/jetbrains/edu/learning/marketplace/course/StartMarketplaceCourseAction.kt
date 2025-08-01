package com.jetbrains.edu.learning.marketplace.course

import com.intellij.openapi.ui.Messages
import com.jetbrains.edu.learning.StartCourseAction
import com.jetbrains.edu.learning.marketplace.MARKETPLACE
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.marketplace.api.EduCourseConnector
import com.jetbrains.edu.learning.stepik.course.ImportCourseDialog

@Suppress("ComponentNotRegistered")
class StartMarketplaceCourseAction : StartCourseAction() {
  override fun courseConnector(): EduCourseConnector = MarketplaceConnector.getInstance()
  override fun createImportCourseDialog(): ImportCourseDialog = StartMarketplaceCourseDialog(courseConnector())

  override fun showFailedToAddCourseNotification(courseLink: String) {
    Messages.showErrorDialog(
      EduCoreBundle.message("error.failed.to.find.course.by.link", MARKETPLACE, courseLink),
      EduCoreBundle.message("error.failed.to.find.course.title", MARKETPLACE)
    )
  }
}
