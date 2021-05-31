package com.jetbrains.edu.learning.marketplace.newProjectUI

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.marketplace.MARKETPLACE
import com.jetbrains.edu.learning.marketplace.MARKETPLACE_COURSES_HELP
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.*
import kotlinx.coroutines.CoroutineScope

class MarketplaceCoursesPanel(
  coursesPlatformProvider: CoursesPlatformProvider,
  scope: CoroutineScope
) : CoursesPanel(coursesPlatformProvider, scope) {

  override fun tabInfo(): TabInfo {
    val linkText = """<a href="$MARKETPLACE_COURSES_HELP">$MARKETPLACE</a>"""
    val infoText = EduCoreBundle.message("marketplace.courses.explanation", linkText)
    return TabInfo(infoText, null)
  }

  override fun createCoursesListPanel() = MarketplaceCoursesListPanel()

  inner class MarketplaceCoursesListPanel : CoursesListWithResetFilters() {

    override fun createCardForNewCourse(course: Course): CourseCardComponent {
      return EduCourseCard(course)
    }
  }
}
