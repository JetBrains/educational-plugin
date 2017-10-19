package com.jetbrains.edu.learning.newproject.ui

import com.intellij.openapi.ui.DialogWrapper
import com.jetbrains.edu.learning.EduPluginConfigurator
import com.jetbrains.edu.learning.courseFormat.Course
import javax.swing.JComponent

class EduCreateNewStepikCourseDialog(private val myCourse: Course) : DialogWrapper(true) {

  private val myPanel: EduCreateNewStepikCoursePanel = EduCreateNewStepikCoursePanel()

  init {
    title = myCourse.name
    setOKButtonText("Join")
    myPanel.bindCourse(myCourse)
    myPanel.setValidationListener(object : EduCreateNewStepikCoursePanel.ValidationListener {
      override fun onInputDataValidated(isInputDataComplete: Boolean) {
        isOKActionEnabled = isInputDataComplete
      }

    })
    init()
  }

  override fun createCenterPanel(): JComponent = myPanel

  override fun doOKAction() {
    val location = myPanel.locationString
    val language = myCourse.languageById
    if (language != null) {
      EduPluginConfigurator.INSTANCE.forLanguage(language)
              ?.getEduCourseProjectGenerator(myCourse)
              ?.createCourseProject(location)

    }
    close(OK_EXIT_CODE)
  }
}