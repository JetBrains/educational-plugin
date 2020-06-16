package com.jetbrains.edu.learning.newproject.ui

import com.intellij.openapi.actionSystem.AnAction
import com.jetbrains.edu.coursecreator.actions.CCNewCourseAction
import com.jetbrains.edu.learning.messages.EduCoreBundle

private const val LEARN_COMMUNITY_COURSES = "https://www.jetbrains.com/help/education/learner-start-guide.html"  // TODO: update link

class CommunityCoursesPanel(coursesProvider: CoursesPlatformProvider) : CoursesPanel(coursesProvider) {

  override fun toolbarAction(): AnAction {
    return CCNewCourseAction(EduCoreBundle.message("course.dialog.create.course"))
  }

  override fun tabInfo(): TabInfo? {
    val infoText = EduCoreBundle.message("community.courses.explanation")
    val linkText = EduCoreBundle.message("community.courses.explanation.link")
    val linkInfo = LinkInfo(linkText, LEARN_COMMUNITY_COURSES)
    return TabInfo(infoText, linkInfo, null)
  }
}
