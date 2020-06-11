package com.jetbrains.edu.learning.newproject.ui

import com.jetbrains.edu.learning.courseFormat.Course
import javax.swing.Icon

/**
 * Specifies information for the tab on [CoursesPanelWithTabs]
 */
interface CoursesPlatformProvider {
  val name: String

  val icon: Icon

  fun getPanel(dialog: BrowseCoursesDialog): CoursesPanel

  suspend fun loadCourses(): List<Course>
}