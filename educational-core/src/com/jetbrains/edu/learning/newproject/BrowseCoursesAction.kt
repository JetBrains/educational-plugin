package com.jetbrains.edu.learning.newproject

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.learning.CoursesProvider
import com.jetbrains.edu.learning.courseLoading.CourseLoader
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.BrowseCoursesDialog

class BrowseCoursesAction : DumbAwareAction(EduCoreBundle.message("browse.courses"),
                                            EduCoreBundle.message("browse.courses.description"),
                                            null) {

  override fun actionPerformed(e: AnActionEvent) {
    val courses = CourseLoader.getCourseInfosUnderProgress { CoursesProvider.loadAllCourses() } ?: return
    BrowseCoursesDialog(courses).show()
  }
}
