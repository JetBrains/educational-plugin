package com.jetbrains.edu.learning.newproject.ui

import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.jetbrains.edu.learning.courseFormat.Course
import javax.swing.JComponent

class BrowseCoursesDialog(val courses: List<Course>, customToolbarActions: DefaultActionGroup? = null) : OpenCourseDialogBase() {
  val panel = CoursesPanel(courses, customToolbarActions)

  init {
    title = "Select Course"
    init()
    panel.addCourseValidationListener(this::setOKActionEnabled)
  }

  override fun getPreferredFocusedComponent(): JComponent? {
    return panel
  }

  override val courseInfo: CourseInfo
    get() = CourseInfo(panel.selectedCourse, panel.locationString, panel.projectSettings)

  override fun createCenterPanel(): JComponent = panel

  override fun setError(error: ErrorState) {
    panel.updateErrorInfo(error)
  }
}
