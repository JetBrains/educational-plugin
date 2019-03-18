package com.jetbrains.edu.learning.stepik.course

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import javax.swing.JComponent

class ImportStepikCourseDialog : DialogWrapper(false) {
    private var coursePanel: ImportStepikCoursePanel = ImportStepikCoursePanel(myDisposable)

    override fun createCenterPanel(): JComponent? = coursePanel.panel

    public override fun doValidate(): ValidationInfo? {
        val isValid = coursePanel.validate()
        if (!isValid) {
            return ValidationInfo("Course link is invalid")
        }

        return null
    }

    override fun getPreferredFocusedComponent(): JComponent? = coursePanel.preferredFocusedComponent

    init {
        title = "Import Stepik Course"
        init()
        coursePanel.setValidationListener (object : ImportStepikCoursePanel.ValidationListener {
          override fun onLoggedIn(isLoggedIn: Boolean) {
            isOKActionEnabled = isLoggedIn
          }
        })
    }

    fun courseLink(): String = coursePanel.courseLink
}