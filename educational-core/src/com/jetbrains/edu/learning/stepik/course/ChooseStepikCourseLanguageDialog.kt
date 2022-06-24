package com.jetbrains.edu.learning.stepik.course

import com.intellij.openapi.ui.DialogWrapper
import com.jetbrains.edu.learning.courseFormat.EduLanguage
import com.jetbrains.edu.learning.messages.EduCoreBundle
import javax.swing.JComponent

class ChooseStepikCourseLanguageDialog(languages: List<EduLanguage>, name: String) : DialogWrapper(false) {
    private val panel: ChooseStepikCourseLanguagePanel = ChooseStepikCourseLanguagePanel(languages, name)

    override fun createCenterPanel(): JComponent? = panel.constructMainPanel()

    init {
      title = EduCoreBundle.message("action.choose.course.language")
        init()
    }

    fun selectedLanguage(): EduLanguage = panel.selectedLanguage
}