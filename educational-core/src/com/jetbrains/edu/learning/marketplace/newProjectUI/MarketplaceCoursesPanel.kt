package com.jetbrains.edu.learning.marketplace.newProjectUI

import com.jetbrains.edu.coursecreator.actions.CCNewCourseAction
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.marketplace.LEARN_MARKETPLACE_COURSES
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
    val infoText = EduCoreBundle.message("marketplace.courses.explanation")
    val linkText = EduCoreBundle.message("course.dialog.learn.more")
    val linkInfo = LinkInfo(linkText, LEARN_MARKETPLACE_COURSES)
    return TabInfo(infoText, linkInfo, null)
  }

  override fun createCoursesListPanel() = CommunityCoursesListPanel()

  inner class CommunityCoursesListPanel : CoursesListWithResetFilters() {

    override fun createCardForNewCourse(course: Course): CourseCardComponent {
      return EduCourseCard(course)
    }
  }
}
