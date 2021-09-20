package com.jetbrains.edu.learning.marketplace.newProjectUI

import com.intellij.openapi.Disposable
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.marketplace.MARKETPLACE
import com.jetbrains.edu.learning.marketplace.MARKETPLACE_COURSES_HELP
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.CourseCardComponent
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProvider
import com.jetbrains.edu.learning.newproject.ui.EduCourseCard
import kotlinx.coroutines.CoroutineScope

class MarketplaceCoursesPanel(
  coursesPlatformProvider: CoursesPlatformProvider,
  scope: CoroutineScope,
  disposable: Disposable
) : CoursesPanel(coursesPlatformProvider, scope, disposable) {

  override fun tabDescription(): String {
    val linkText = """<a href="$MARKETPLACE_COURSES_HELP">$MARKETPLACE</a>"""
    return EduCoreBundle.message("marketplace.courses.explanation", linkText)
  }

  override fun createCoursesListPanel() = MarketplaceCoursesListPanel()

  inner class MarketplaceCoursesListPanel : CoursesListWithResetFilters() {

    override fun createCardForNewCourse(course: Course): CourseCardComponent {
      return EduCourseCard(course)
    }
  }
}
