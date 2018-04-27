package com.jetbrains.edu.coursecreator.ui

import com.intellij.openapi.ui.DialogWrapper
import com.jetbrains.edu.learning.EduConfiguratorManager
import com.jetbrains.edu.learning.courseFormat.Course
import javax.swing.JComponent

class CCNewCourseDialog(
  title: String,
  okButtonText: String,
  course: Course? = null
) : DialogWrapper(true) {

  private val myPanel: CCNewCoursePanel = CCNewCoursePanel(course)

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
    val language = course.languageById
    if (language != null) {
      EduConfiguratorManager.forLanguage(language)
        ?.courseBuilder
        ?.getCourseProjectGenerator(course)
        ?.doCreateCourseProject(location, projectSettings)
    }
  }
}