package com.jetbrains.edu.coursecreator.ui

import com.intellij.openapi.ui.DialogWrapper
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import javax.swing.JComponent

class CCNewCourseDialog(
  title: String,
  okButtonText: String,
  course: Course? = null,
  courseProducer: () -> Course = ::EduCourse
) : DialogWrapper(true) {

  private val myPanel: CCNewCoursePanel = CCNewCoursePanel(course, courseProducer)

  init {
    setTitle(title)
    setOKButtonText(okButtonText)
    init()
    myPanel.setValidationListener(object : CCNewCoursePanel.ValidationListener {
      override fun onInputDataValidated(isInputDataComplete: Boolean) {
        isOKActionEnabled = isInputDataComplete
      }
    })
  }

  override fun createCenterPanel(): JComponent = myPanel

  override fun doOKAction() {
    close(OK_EXIT_CODE)
    val course = myPanel.course
    val projectSettings = myPanel.projectSettings
    val location = myPanel.locationString
    course.configurator
      ?.courseBuilder
      ?.getCourseProjectGenerator(course)
      ?.doCreateCourseProject(location, projectSettings)
  }
}