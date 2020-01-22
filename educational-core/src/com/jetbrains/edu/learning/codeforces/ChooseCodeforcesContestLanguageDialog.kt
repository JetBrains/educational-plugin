package com.jetbrains.edu.learning.codeforces

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.layout.*
import com.intellij.util.ui.JBUI
import javax.swing.JCheckBox
import javax.swing.JComponent

class ChooseCodeforcesContestLanguageDialog(private val contestName: String, languagesList: List<String>) :
  DialogWrapper(false) {
  private val languageComboBox: ComboBox<String> = ComboBox()
  private val languagePreferenceCheckBox: JCheckBox = JCheckBox("Save language as preferable")

  init {
    title = "Choose Contest Language"
    languagesList.sorted().forEach {
      languageComboBox.addItem(it)
    }
    init()
  }

  override fun createCenterPanel(): JComponent? = panel {
    row("Contest:") {
      label(contestName)
    }
    row("Language:") {
      languageComboBox()
    }
    row {
      languagePreferenceCheckBox()
    }

  }.apply {
    preferredSize = JBUI.size(400, 50)
  }

  override fun getPreferredFocusedComponent(): JComponent? = languageComboBox

  fun selectedLanguage(): String = languageComboBox.selectedItem as String

  fun selectLanguage(language: String) {
    languageComboBox.selectedItem = language
  }

  fun isSaveLanguageAsPreferable(): Boolean = languagePreferenceCheckBox.isSelected
}