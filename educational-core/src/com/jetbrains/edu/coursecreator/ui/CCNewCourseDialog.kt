package com.jetbrains.edu.coursecreator.ui

import com.intellij.openapi.ui.DialogWrapper
import com.jetbrains.edu.learning.EduPluginConfiguratorManager
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
    val projectSettings = myPanel.projectSettings
    val location = myPanel.locationString
    val language = course.languageById
    if (language != null) {
      EduPluginConfiguratorManager.forLanguage(language)
              ?.getEduCourseProjectGenerator(course)
              ?.createCourseProject(location, projectSettings)
    }
    close(OK_EXIT_CODE)
  }
}
