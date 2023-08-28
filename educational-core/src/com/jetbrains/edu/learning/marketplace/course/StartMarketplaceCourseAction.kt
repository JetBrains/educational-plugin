package com.jetbrains.edu.learning.marketplace.course

import com.jetbrains.edu.learning.StartCourseAction
import com.jetbrains.edu.learning.marketplace.MARKETPLACE
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.stepik.course.CourseConnector
import com.jetbrains.edu.learning.stepik.course.ImportCourseDialog

@Suppress("ComponentNotRegistered")
class StartMarketplaceCourseAction : StartCourseAction(MARKETPLACE) {
  override val dialog: ImportCourseDialog
    get() = StartMarketplaceCourseDialog(courseConnector)
  override val courseConnector: CourseConnector = MarketplaceConnector.getInstance()
}