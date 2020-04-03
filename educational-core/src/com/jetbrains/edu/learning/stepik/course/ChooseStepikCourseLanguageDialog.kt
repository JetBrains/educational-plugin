package com.jetbrains.edu.learning.stepik.course

import com.intellij.openapi.ui.DialogWrapper
import com.jetbrains.edu.learning.stepik.StepikLanguage
import javax.swing.JComponent

class ChooseStepikCourseLanguageDialog(languages: List<StepikLanguage>, name: String) : DialogWrapper(false) {
    private val panel: ChooseStepikCourseLanguagePanel = ChooseStepikCourseLanguagePanel(languages, name)

    override fun createCenterPanel(): JComponent? = panel.constructMainPanel()

    init {
        title = "Choose Course Language"
        init()
    }

    fun selectedLanguage(): StepikLanguage = panel.selectedLanguage
}