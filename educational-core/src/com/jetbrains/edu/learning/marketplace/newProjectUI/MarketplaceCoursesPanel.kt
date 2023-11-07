package com.jetbrains.edu.learning.marketplace.newProjectUI

import com.intellij.openapi.Disposable
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.marketplace.MARKETPLACE
import com.jetbrains.edu.learning.marketplace.MARKETPLACE_COURSES_HELP
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.coursesList.CourseCardComponent
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel
import com.jetbrains.edu.learning.newproject.ui.platformProviders.CoursesPlatformProvider
import com.jetbrains.edu.learning.newproject.ui.coursesList.EduCourseCard
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CoursePanel
import kotlinx.coroutines.CoroutineScope

class MarketplaceCoursesPanel(
  coursesPlatformProvider: CoursesPlatformProvider,
  scope: CoroutineScope,
  disposable: Disposable
) : CoursesPanel(coursesPlatformProvider, scope, disposable) {

  override fun createCoursePanel(disposable: Disposable): CoursePanel {
    return MarketplaceCoursePanel(disposable)
  }

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
