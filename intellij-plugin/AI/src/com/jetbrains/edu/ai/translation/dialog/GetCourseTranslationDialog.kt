package com.jetbrains.edu.ai.translation.dialog

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.toNullableProperty
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.ai.messages.EduAIBundle
import com.jetbrains.edu.ai.translation.settings.TranslationSettings
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.educational.translation.enum.Language
import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox
import javax.swing.JComponent

class GetCourseTranslationDialog(private val course: EduCourse) : DialogWrapper(true) {
  private val courseSourceLanguage = Language.findByCode(course.languageCode)
  private var selectedLanguage = TranslationSettings.getInstance().preferableLanguage

  private lateinit var comboBox: JComboBox<Language>

  init {
    title = EduAIBundle.message("action.Educational.GetCourseTranslation.text")

    isOKActionEnabled = selectedLanguage != courseSourceLanguage
    isResizable = true

    setOKButtonText(EduCoreBundle.message("button.select"))
    init()
  }

  override fun createCenterPanel(): JComponent = com.intellij.ui.dsl.builder.panel {
    row(EduAIBundle.message("ai.service.translation.course.language.label")) {
      label(course.humanLanguage)
    }
    row(EduAIBundle.message("ai.service.translation.select.target.language.label")) {
      comboBox = comboBox(LanguageComboBoxModel())
        .bindItem(::selectedLanguage.toNullableProperty())
        .onChanged {
          isOKActionEnabled = it.selectedItem != courseSourceLanguage
        }
        .focused()
        .align(Align.FILL)
        .component
    }
  }.apply {
    preferredSize = JBUI.size(WIDTH, HEIGHT)
    minimumSize = JBUI.size(WIDTH, HEIGHT)
  }

  fun getLanguage(): Language? {
    if (!showAndGet()) return null
    return selectedLanguage
  }

  private inner class LanguageComboBoxModel : DefaultComboBoxModel<Language>() {
    init {
      @OptIn(ExperimentalStdlibApi::class)
      val languages = Language.entries
        .filter { it != courseSourceLanguage }
        .sortedBy { it.label }

      addAll(languages)
    }
  }

  companion object {
    private const val WIDTH: Int = 350
    private const val HEIGHT: Int = 72
  }
}