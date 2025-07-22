package com.jetbrains.edu.learning.marketplace.course

import com.jetbrains.edu.learning.StartCourseAction
import com.jetbrains.edu.learning.marketplace.MARKETPLACE
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.stepik.course.CourseConnector
import com.jetbrains.edu.learning.stepik.course.ImportCourseDialog

class StartMarketplaceCourseAction : StartCourseAction(MARKETPLACE) {
  override fun courseConnector(): CourseConnector = MarketplaceConnector.getInstance()
  override fun createImportCourseDialog(): ImportCourseDialog = StartMarketplaceCourseDialog(courseConnector())
}
