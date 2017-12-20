package com.jetbrains.edu.learning.stepik.newproject

import com.intellij.openapi.ui.DialogWrapper
import com.jetbrains.edu.learning.EduConfiguratorManager
import com.jetbrains.edu.learning.courseFormat.Course
import javax.swing.JComponent

class CreateNewStepikCourseDialog(private val myCourse: Course) : DialogWrapper(true) {

  private val myPanel: CreateNewStepikCoursePanel = CreateNewStepikCoursePanel()

  init {
    title = myCourse.name
    setOKButtonText("Join")
    myPanel.bindCourse(myCourse)
    myPanel.setValidationListener(object : CreateNewStepikCoursePanel.ValidationListener {
      override fun onInputDataValidated(isInputDataComplete: Boolean) {
        isOKActionEnabled = isInputDataComplete
      }

    })
    init()
  }

  override fun createCenterPanel(): JComponent = myPanel

  override fun doOKAction() {
    val location = myPanel.locationString
    val projectSettings = myPanel.projectSettings
    val language = myCourse.languageById
    if (language != null) {
      EduConfiguratorManager.forLanguage(language)
              ?.courseBuilder
              ?.getCourseProjectGenerator(myCourse)
              ?.doCreateCourseProject(location, projectSettings)
    }
    close(OK_EXIT_CODE)
  }
}