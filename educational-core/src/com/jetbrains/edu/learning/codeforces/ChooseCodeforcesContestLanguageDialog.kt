package com.jetbrains.edu.learning.codeforces

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.layout.*
import com.intellij.util.ui.JBUI
import javax.swing.JComponent

class ChooseCodeforcesContestLanguageDialog(private val contestName: String, languagesList: List<String>) :
  DialogWrapper(false) {
  val languageComboBox: ComboBox<String> = ComboBox()

  init {
    title = "Choose Contest Language"
    languagesList.forEach {
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
  }.apply {
    preferredSize = JBUI.size(400, 50)
  }

  override fun getPreferredFocusedComponent(): JComponent? = languageComboBox

  fun selectedProgrammingLanguage(): String = languageComboBox.selectedItem as String
}