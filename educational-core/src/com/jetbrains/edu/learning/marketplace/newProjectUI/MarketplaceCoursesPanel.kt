package com.jetbrains.edu.learning.marketplace.newProjectUI

import com.jetbrains.edu.coursecreator.actions.CCNewCourseAction
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

  override fun toolbarAction(): ToolbarActionWrapper {
    return ToolbarActionWrapper(EduCoreBundle.lazyMessage("course.dialog.create.course"), CCNewCourseAction())
  }

  override fun tabInfo(): TabInfo {
    val linkText = """<a href="$MARKETPLACE_COURSES_HELP">$MARKETPLACE</a>"""
    val infoText = EduCoreBundle.message("marketplace.courses.explanation", linkText)
    return TabInfo(infoText, null)
  }

  override fun createCoursesListPanel() = CommunityCoursesListPanel()

  inner class CommunityCoursesListPanel : CoursesListWithResetFilters() {

    override fun createCardForNewCourse(course: Course): CourseCardComponent {
      return EduCourseCard(course)
    }
  }
}
