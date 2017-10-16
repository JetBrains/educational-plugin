package com.jetbrains.edu.learning.newproject.ui

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import javax.swing.JComponent

class ImportStepikCourseDialog : DialogWrapper(false) {
    private var coursePanel: ImportStepikCoursePanel

    override fun createCenterPanel(): JComponent? = coursePanel.mainPanel

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
        coursePanel = ImportStepikCoursePanel()
        init()
    }

    fun courseLink(): String = coursePanel.courseLink
}