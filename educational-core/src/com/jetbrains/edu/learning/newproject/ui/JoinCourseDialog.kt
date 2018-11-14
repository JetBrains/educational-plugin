package com.jetbrains.edu.learning.newproject.ui

import com.jetbrains.edu.learning.courseFormat.Course
import javax.swing.JComponent

class JoinCourseDialog(private val course: Course) : OpenCourseDialogBase() {

  private val panel: JoinCoursePanel = JoinCoursePanel()

  init {
    title = course.name
    panel.bindCourse(course)
    panel.setValidationListener(object : JoinCoursePanel.ValidationListener {
      override fun onInputDataValidated(isInputDataComplete: Boolean) {
        isOKActionEnabled = isInputDataComplete
      }

    })
    init()
  }

  override val courseInfo: CourseInfo
    get() = CourseInfo(course, panel.locationString, panel.projectSettings)

  override fun createCenterPanel(): JComponent = panel
}
