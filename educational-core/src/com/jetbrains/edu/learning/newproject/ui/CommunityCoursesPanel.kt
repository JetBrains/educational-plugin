package com.jetbrains.edu.learning.newproject.ui

import com.jetbrains.edu.coursecreator.actions.CCNewCourseAction
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.CoursesGroup
import com.jetbrains.edu.learning.stepik.api.StepikCoursesProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val LEARN_COMMUNITY_COURSES = "https://www.jetbrains.com/help/education/learner-start-guide.html"  // TODO: update link

class CommunityCoursesPanel(coursesPlatformProvider: CoursesPlatformProvider,
                            private val stepikCoursesProvider: StepikCoursesProvider,
                            scope: CoroutineScope) : CoursesPanel(coursesPlatformProvider, scope) {

  override fun toolbarAction(): ToolbarActionWrapper {
    return ToolbarActionWrapper(EduCoreBundle.lazyMessage("course.dialog.create.course"), CCNewCourseAction())
  }

  override fun tabInfo(): TabInfo {
    val infoText = EduCoreBundle.message("community.courses.explanation")
    val linkText = EduCoreBundle.message("course.dialog.learn.more")
    val linkInfo = LinkInfo(linkText, LEARN_COMMUNITY_COURSES)
    return TabInfo(infoText, linkInfo, null)
  }

  override suspend fun updateCoursesAfterLogin(preserveSelection: Boolean) {
    val privateCourses = withContext(Dispatchers.IO) { stepikCoursesProvider.loadPrivateCourseInfos() }
    val privateCoursesGroup = CoursesGroup(EduCoreBundle.message("course.dialog.private.courses.group"), privateCourses)
    coursesGroups.add(0, privateCoursesGroup)
    super.updateCoursesAfterLogin(preserveSelection)
  }
}
