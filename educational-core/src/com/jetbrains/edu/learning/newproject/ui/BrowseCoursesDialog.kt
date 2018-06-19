package com.jetbrains.edu.learning.newproject.ui

import com.intellij.openapi.ui.DialogWrapper
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.OpenCourseAction
import javax.swing.JComponent

class BrowseCoursesDialog(val courses: List<Course>) : DialogWrapper(true) {

  val panel = CoursesPanel(courses)

  init {
    title = "Select Course"
    myOKAction = OpenCourseAction(this)
    init()
    panel.addCourseValidationListener(this::setOKActionEnabled)
  }

  override fun createCenterPanel(): JComponent = panel

  val selectedCourse: Course get() = panel.selectedCourse
  val projectSettings: Any get() = panel.projectSettings
  val locationString: String get() = panel.locationString
}
