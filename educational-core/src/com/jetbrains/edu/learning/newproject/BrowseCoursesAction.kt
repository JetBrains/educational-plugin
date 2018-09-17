package com.jetbrains.edu.learning.newproject

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.learning.courseLoading.CourseLoader
import com.jetbrains.edu.learning.newproject.ui.BrowseCoursesDialog

class BrowseCoursesAction : DumbAwareAction("Browse Courses", "Browse list of available courses", null) {

  override fun actionPerformed(e: AnActionEvent) {
    val courses = CourseLoader.getCourseInfosUnderProgress() ?: return
    BrowseCoursesDialog(courses).show()
  }
}
