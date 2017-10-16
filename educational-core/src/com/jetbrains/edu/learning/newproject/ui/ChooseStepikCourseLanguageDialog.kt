package com.jetbrains.edu.learning.newproject.ui

import com.intellij.lang.Language
import com.intellij.openapi.ui.DialogWrapper
import javax.swing.JComponent

class ChooseStepikCourseLanguageDialog(languages: List<Language>, name: String) : DialogWrapper(false) {
    private val panel: ChooseStepikCourseLanguagePanel = ChooseStepikCourseLanguagePanel(languages, name)

    override fun createCenterPanel(): JComponent? = panel.constructMainPanel()

    init {
        title = "Choose Course Language"
        init()
    }

    fun selectedLanguage(): Language = panel.selectedLanguage
}