package com.jetbrains.edu.learning.codeforces

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.layout.*
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.coursecreator.getDefaultLanguageId
import javax.swing.JCheckBox
import javax.swing.JComponent

// TODO move text of labels to message bundle
class ChooseCodeforcesContestLanguagesDialog(private val contestInformation: ContestInformation) : DialogWrapper(false) {
  private val textLanguageComboBox: ComboBox<TaskTextLanguage> = ComboBox()
  private val languageComboBox: ComboBox<String> = ComboBox()
  private val doNotShowLanguageDialogCheckBox: JCheckBox = JCheckBox("Do not ask if selected languages are available")
  private val comboBoxesWidth: Int = JBUI.scale(130)

  init {
    title = "Choose Contest Languages"
    initTextLanguageComboBox()
    initLanguageComboBox()
    init()
  }

  override fun createCenterPanel(): JComponent? = panel {
    row {
      label("Contest: ${contestInformation.name}")
    }
    row {
      row("Language:") {
        textLanguageComboBox()
      }
      row("Programming language:") {
        languageComboBox()
      }
      row {
        doNotShowLanguageDialogCheckBox()
      }
    }
  }

  override fun getPreferredFocusedComponent(): JComponent? = languageComboBox

  fun selectedTaskTextLanguage(): TaskTextLanguage = textLanguageComboBox.selectedItem as TaskTextLanguage

  fun selectedLanguage(): String = languageComboBox.selectedItem as String

  fun isDoNotShowLanguageDialog(): Boolean = doNotShowLanguageDialogCheckBox.isSelected

  private fun initTextLanguageComboBox() {
    TaskTextLanguage.values().forEach {
      textLanguageComboBox.addItem(it)
    }

    val preferableTaskTextLanguage = CodeforcesSettings.getInstance().preferableTaskTextLanguage
    if (preferableTaskTextLanguage != null && preferableTaskTextLanguage in TaskTextLanguage.values()) {
      textLanguageComboBox.selectedItem = preferableTaskTextLanguage
    }

    textLanguageComboBox.setMinimumAndPreferredWidth(comboBoxesWidth)
  }

  private fun initLanguageComboBox() {
    contestInformation.availableLanguages.sorted().forEach {
      languageComboBox.addItem(it)
    }

    val preferableLanguage = CodeforcesSettings.getInstance().preferableLanguage
    if (preferableLanguage != null && preferableLanguage in contestInformation.availableLanguages) {
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
}