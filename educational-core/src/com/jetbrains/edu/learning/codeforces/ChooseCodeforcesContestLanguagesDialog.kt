package com.jetbrains.edu.learning.codeforces

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.layout.*
import javax.swing.JCheckBox
import javax.swing.JComponent

// TODO move text of labels to message bundle
class ChooseCodeforcesContestLanguagesDialog(private val contestName: String, private val languagesList: List<String>) :
  DialogWrapper(false) {
  private val textLanguageComboBox = ComboBox<TaskTextLanguage>()
  private val languageComboBox: ComboBox<String> = ComboBox()
  private val languagePreferenceCheckBox: JCheckBox = JCheckBox("Save language preferences")

  init {
    title = "Choose Contest Languages"
    initTextLanguageComboBox()
    initLanguageComboBox()
    init()
  }

  override fun createCenterPanel(): JComponent? = panel {
    row {
      cell(isFullWidth = true) {
        label("Contest:")
        label(contestName)
      }
    }
    row {
      row("Language:") {
        textLanguageComboBox()
      }
      row("Programming language:") {
        languageComboBox()
      }
      row {
        languagePreferenceCheckBox()
      }
    }
  }

  override fun getPreferredFocusedComponent(): JComponent? = languageComboBox

  fun selectedTaskTextLanguage(): TaskTextLanguage = textLanguageComboBox.selectedItem as TaskTextLanguage

  fun selectedLanguage(): String = languageComboBox.selectedItem as String

  fun isSavePreferences(): Boolean = languagePreferenceCheckBox.isSelected

  private fun initTextLanguageComboBox() {
    TaskTextLanguage.values().forEach {
      textLanguageComboBox.addItem(it)
    }

    val preferableTextLanguage = CodeforcesSettings.getInstance().codeforcesPreferableTextLanguage
    if (preferableTextLanguage != null && preferableTextLanguage in TaskTextLanguage.values().map { it.name }) {
      textLanguageComboBox.selectedItem = TaskTextLanguage.valueOf(preferableTextLanguage)
    }
  }

  private fun initLanguageComboBox() {
    languagesList.sorted().forEach {
      languageComboBox.addItem(it)
    }

    val preferableLanguage = CodeforcesSettings.getInstance().codeforcesPreferableLanguage
    if (preferableLanguage != null && preferableLanguage in languagesList) {
      languageComboBox.selectedItem = preferableLanguage
    }
  }
}