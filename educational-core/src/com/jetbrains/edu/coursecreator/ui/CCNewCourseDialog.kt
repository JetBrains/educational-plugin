package com.jetbrains.edu.coursecreator.ui

import com.intellij.openapi.ui.DialogWrapper
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import org.jetbrains.annotations.Nls
import javax.swing.JComponent

class CCNewCourseDialog(
  @Nls(capitalization = Nls.Capitalization.Title) title: String,
  @Nls(capitalization = Nls.Capitalization.Title) okButtonText: String,
  course: Course? = null,
  courseProducer: () -> Course = ::EduCourse,
  private val onOKAction: () -> Unit = {}
) : DialogWrapper(true) {

  private val panel: CCNewCoursePanel = CCNewCoursePanel(disposable, course, courseProducer)

  init {
    setTitle(title)
    setOKButtonText(okButtonText)
    init()
    panel.setValidationListener(object : CCNewCoursePanel.ValidationListener {
      override fun onInputDataValidated(isInputDataComplete: Boolean) {
        isOKActionEnabled = isInputDataComplete
      }
    })
  }

  override fun createCenterPanel(): JComponent = panel

  override fun doOKAction() {
    panel.validateLocation()
    if (!isOKActionEnabled) {
      return
    }
    close(OK_EXIT_CODE)
    onOKAction()
    val course = panel.course
    val projectSettings = panel.projectSettings
    val location = panel.locationString
    course.configurator
      ?.courseBuilder
      ?.getCourseProjectGenerator(course)
      ?.doCreateCourseProject(location, projectSettings)
  }
}