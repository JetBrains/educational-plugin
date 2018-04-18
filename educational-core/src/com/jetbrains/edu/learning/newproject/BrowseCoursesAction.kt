package com.jetbrains.edu.learning.newproject

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.newproject.ui.BrowseCoursesDialog
import icons.EducationalCoreIcons

class BrowseCoursesAction : DumbAwareAction("Browse Courses", "Browse list of available courses", EducationalCoreIcons.CourseAction) {

  override fun actionPerformed(e: AnActionEvent) {
    val courses = EduUtils.getCoursesUnderProgress() ?: return
    BrowseCoursesDialog(courses).show()
  }
}
