package com.jetbrains.edu.coursecreator.ui

import com.intellij.openapi.ui.DialogWrapper
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
    val location = myPanel.locationString
    myPanel.projectGenerator?.createCourseProject(location)
    close(OK_EXIT_CODE)
  }
}
