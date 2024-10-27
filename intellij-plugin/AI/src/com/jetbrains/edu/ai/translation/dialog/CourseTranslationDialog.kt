package com.jetbrains.edu.ai.translation.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.*
import com.intellij.util.application
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.ai.messages.EduAIBundle
import com.jetbrains.edu.ai.translation.settings.translationSettings
import com.jetbrains.edu.learning.ai.TranslationProjectSettings
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.ui.EduColors
import com.jetbrains.educational.core.enum.Language
import javax.swing.*
import javax.swing.JPanel.LEFT_ALIGNMENT

class CourseTranslationDialog(private val project: Project, course: EduCourse) : DialogWrapper(true) {
  private val courseSourceLanguage = Language.findByCode(course.languageCode)

  private var selectedLanguage: Language
  private lateinit var translateToCheckBox: Cell<JBCheckBox>
  private lateinit var translationLanguageComboBox: JComboBox<Language>

  init {
    title = EduAIBundle.message("ai.translation.course.translation.dialog.title")

    val currentTranslationLanguage = TranslationProjectSettings.getCurrentTranslationLanguage(project)
    selectedLanguage = if (currentTranslationLanguage?.isNotSource() == true) {
      currentTranslationLanguage
    } else {
      application.translationSettings().preferableLanguage
    }

    isResizable = true
    init()
  }

  override fun createActions(): Array<Action> = emptyArray()

  override fun createCenterPanel(): JComponent = panel {
    row {
      translateToCheckBox = translateToCheckBox()
      translationLanguageComboBox = translationLanguageComboBox()
    }
    row {
      bottomLabel()
    }
  }.apply {
    preferredSize = JBUI.size(WIDTH, HEIGHT)
    minimumSize = JBUI.size(WIDTH, HEIGHT)
  }

  fun getLanguage(): Language? {
    if (!showAndGet()) return null
    return selectedLanguage
  }

  private fun Row.translateToCheckBox(): Cell<JBCheckBox> =
    checkBox(EduAIBundle.message("ai.translation.translate.to.label"))
      .applyToComponent {
        alignmentX = LEFT_ALIGNMENT
      }
      .apply {
        enabled(selectedLanguage.isNotSource())
        selected(TranslationProjectSettings.isCourseTranslated(project))
      }
      .onChanged {
        if (it.isSelected && selectedLanguage.isNew()) {
          close(OK_EXIT_CODE)
        }
        else if (!it.isSelected) {
          TranslationProjectSettings.resetTranslation(project)
          close(CANCEL_EXIT_CODE)
        }
      }

  private fun Row.translationLanguageComboBox(): JComboBox<Language> =
    comboBox(LanguageComboBoxModel())
      .bindItem(::selectedLanguage.toNullableProperty())
      .onChanged {
        selectedLanguage = it.selectedItem as? Language ?: return@onChanged
        if (selectedLanguage.isNew()) {
          close(OK_EXIT_CODE)
        }
        translateToCheckBox.enabled(selectedLanguage.isNotSource())
      }
      .focused()
      .align(Align.FILL)
      .component

  private fun Row.bottomLabel(): Cell<JLabel> =
    label(EduAIBundle.message("ai.translation.ai.powered.translation.to.multiple.languages"))
      .applyToComponent {
        alignmentX = LEFT_ALIGNMENT
        foreground = EduColors.aiTranslationBottomLabelTextColor
      }

  private fun Language?.isNotSource(): Boolean = this != courseSourceLanguage

  private fun Language.isNew(): Boolean = isNotSource() && this != TranslationProjectSettings.getCurrentTranslationLanguage(project)

  private inner class LanguageComboBoxModel : DefaultComboBoxModel<Language>() {
    init {
      @OptIn(ExperimentalStdlibApi::class)
      val languages = Language.entries
        .filter { it != courseSourceLanguage }
        .sortedBy { it.label }

      addAll(languages)
      selectedItem = EduAIBundle.message("ai.translation.choose.language")
    }
  }

  companion object {
    private const val WIDTH: Int = 350
    private const val HEIGHT: Int = 72
  }
}