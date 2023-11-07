package com.jetbrains.edu.learning.courseGeneration

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.ui.dialogs.JoinCourseDialog
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseDisplaySettings
import com.jetbrains.edu.learning.stepik.builtInServer.EduBuiltInServerUtils

class ProjectOpenerImpl : ProjectOpener() {

  override fun newProject(course: Course): Boolean {
    return JoinCourseDialog(course, CourseDisplaySettings(showTagsPanel = false, showInstructorField = false)).showAndGet()
  }

  override fun focusOpenProject(coursePredicate: (Course) -> Boolean): Pair<Project, Course>? =
    EduBuiltInServerUtils.focusOpenProject(coursePredicate)
}