package com.jetbrains.edu.learning.stepik.course

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import javax.swing.JComponent

abstract class ImportCourseDialog : DialogWrapper(false) {
  abstract val coursePanel: ImportCoursePanel

  override fun getPreferredFocusedComponent(): JComponent = coursePanel.preferredFocusedComponent

  override fun createCenterPanel(): JComponent = coursePanel.panel

  public override fun doValidate(): ValidationInfo? {
    val isValid = coursePanel.validate()
    if (!isValid) {
      return ValidationInfo("Course link is invalid")
    }

    return null
  }

  fun courseLink(): String = coursePanel.courseLink
}