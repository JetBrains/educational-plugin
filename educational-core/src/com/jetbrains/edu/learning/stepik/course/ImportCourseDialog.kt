package com.jetbrains.edu.learning.stepik.course

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.jetbrains.edu.learning.messages.EduCoreBundle
import javax.swing.JComponent

abstract class ImportCourseDialog : DialogWrapper(false) {
  abstract val coursePanel: ImportCoursePanel

  override fun getPreferredFocusedComponent(): JComponent = coursePanel.preferredFocusedComponent

  override fun createCenterPanel(): JComponent = coursePanel.panel

  public override fun doValidate(): ValidationInfo? {
    val isValid = coursePanel.validate()
    if (!isValid) {
      return ValidationInfo(EduCoreBundle.message("action.import.local.course.dialog.invalid.link"))
    }

    return null
  }

  fun courseLink(): String = coursePanel.courseLink
}