package com.jetbrains.edu.learning.marketplace.course

import com.jetbrains.edu.learning.StartCourseAction
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.marketplace.MARKETPLACE
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.newProjectUI.MarketplaceCoursePanel
import com.jetbrains.edu.learning.newproject.ui.JoinCourseDialog
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CoursePanel
import com.jetbrains.edu.learning.stepik.course.CourseConnector
import com.jetbrains.edu.learning.stepik.course.ImportCourseDialog

@Suppress("ComponentNotRegistered")
class StartMarketplaceCourseAction : StartCourseAction(MARKETPLACE) {

  override fun courseConnector(): CourseConnector = MarketplaceConnector.getInstance()
  override fun createImportCourseDialog(): ImportCourseDialog = StartMarketplaceCourseDialog(courseConnector())
  override fun createJoinCourseDialog(course: Course): JoinCourseDialog = MarketplaceJoinCourseDialog(course)

  private class MarketplaceJoinCourseDialog(course: Course) : JoinCourseDialog(course) {
    override fun createCoursePanel(): CoursePanel {
      return MarketplaceCoursePanel(disposable)
    }
  }
}
