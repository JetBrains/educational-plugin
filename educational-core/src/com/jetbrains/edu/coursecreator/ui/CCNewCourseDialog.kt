package com.jetbrains.edu.coursecreator.ui

import com.intellij.openapi.ui.DialogWrapper
import com.jetbrains.edu.learning.EduPluginConfigurator
import javax.swing.JComponent

class CCNewCourseDialog : DialogWrapper(true) {

  private val myPanel: CCNewCoursePanel = CCNewCoursePanel()

  init {
    title = "Create Course"
    setOKButtonText("Create")
    init()
    myPanel.setValidationListener(object : CCNewCoursePanel.ValidationListener {
      override fun onInputDataValidated(isInputDataComplete: Boolean) {
        isOKActionEnabled = isInputDataComplete
      }
    })
  }

  override fun createCenterPanel(): JComponent = myPanel

  override fun doOKAction() {
    val course = myPanel.course
    val location = myPanel.locationString
    val language = course.languageById
    if (language != null) {
      val configurator = EduPluginConfigurator.INSTANCE.forLanguage(language)
      if (configurator != null) {
        val projectGenerator = configurator.eduCourseProjectGenerator
        projectGenerator?.createProject(course, location)
      }
    }
    close(OK_EXIT_CODE)
  }
}
