package com.jetbrains.edu.learning.codeforces

import com.intellij.openapi.extensions.BaseExtensionPointName
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.layout.*
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.coursecreator.getDefaultLanguageId
import com.jetbrains.edu.learning.messages.EduCoreBundle
import javax.swing.JCheckBox
import javax.swing.JComponent

class CodeforcesOptions : Configurable, Configurable.WithEpDependencies {
  private val textLanguageComboBox: ComboBox<TaskTextLanguage> = ComboBox()
  private val languageComboBox: ComboBox<String> = ComboBox()
  private val doNotShowLanguageDialogCheckBox: JCheckBox = JCheckBox(EduCoreBundle.message("codeforces.prefer.selected.languages"))
  private val comboBoxesWidth: Int = JBUI.scale(130)
  private val state: State

  init {
    val codeforcesSettings = CodeforcesSettings.getInstance()
    initTextLanguageComboBox(codeforcesSettings)
    initLanguageComboBox(codeforcesSettings)
    state = State(getTaskTextLanguage(), getLanguage(), getDoNotShowLanguageDialog())
  }

  data class State(var textLanguage: TaskTextLanguage, var language: String, var doNotShowLanguageDialog: Boolean)

  private fun initTextLanguageComboBox(codeforcesSettings: CodeforcesSettings) {
    TaskTextLanguage.values().forEach {
      textLanguageComboBox.addItem(it)
    }

    val preferableTaskTextLanguage = codeforcesSettings.preferableTaskTextLanguage
    if (preferableTaskTextLanguage != null) {
      textLanguageComboBox.selectedItem = preferableTaskTextLanguage
    }

    textLanguageComboBox.setMinimumAndPreferredWidth(comboBoxesWidth)
  }

  private fun initLanguageComboBox(codeforcesSettings: CodeforcesSettings) {
    val languages = CodeforcesLanguageProvider.getSupportedLanguages().sorted()
    languages.forEach {
      languageComboBox.addItem(it)
    }

    val preferableLanguage = codeforcesSettings.preferableLanguage
    if (preferableLanguage != null && preferableLanguage in languages) {
      languageComboBox.selectedItem = preferableLanguage
    }
    else {
      val defaultLanguageId = getDefaultLanguageId()
      if (defaultLanguageId != null) {
        languageComboBox.selectedItem = CodeforcesLanguageProvider.getPreferableCodeforcesLanguage(defaultLanguageId)
      }
    }

    languageComboBox.setMinimumAndPreferredWidth(comboBoxesWidth)
  }

  override fun getDependencies(): Collection<BaseExtensionPointName<*>> {
    return listOf(CodeforcesLanguageProvider.EP_NAME)
  }

  override fun getDisplayName(): String = CodeforcesNames.CODEFORCES_TITLE

  override fun apply() {
    val textLanguage = getTaskTextLanguage()
    val language = getLanguage()
    val doNotShowLanguageDialog = getDoNotShowLanguageDialog()

    val codeforcesSettings = CodeforcesSettings.getInstance()
    codeforcesSettings.preferableTaskTextLanguage = textLanguage
    codeforcesSettings.preferableLanguage = language

    state.textLanguage = textLanguage
    state.language = language
    state.doNotShowLanguageDialog = doNotShowLanguageDialog
  }

  override fun createComponent(): JComponent = panel {
    row("${EduCoreBundle.message("label.codeforces.language")}:") {
      textLanguageComboBox()
    }
    row("${EduCoreBundle.message("label.codeforces.programming.language")}:") {
      languageComboBox()
    }
    row {
      doNotShowLanguageDialogCheckBox()
    }
  }

  override fun reset() {
    textLanguageComboBox.selectedItem = state.textLanguage
    languageComboBox.selectedItem = state.language
    doNotShowLanguageDialogCheckBox.isSelected = state.doNotShowLanguageDialog
  }

  override fun isModified(): Boolean = state.textLanguage != getTaskTextLanguage()
                                       || state.language != getLanguage()
                                       || state.doNotShowLanguageDialog != getDoNotShowLanguageDialog()

  private fun getTaskTextLanguage(): TaskTextLanguage = textLanguageComboBox.selectedItem as TaskTextLanguage

  private fun getLanguage(): String = languageComboBox.selectedItem!!.toString()

  private fun getDoNotShowLanguageDialog(): Boolean = doNotShowLanguageDialogCheckBox.isSelected
}
