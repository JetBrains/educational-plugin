package com.jetbrains.edu.learning.codeforces

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.layout.*
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.coursecreator.getDefaultLanguageId
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle
import javax.swing.JCheckBox
import javax.swing.JComponent

class ChooseCodeforcesContestLanguagesDialog(private val codeforcesCourse: CodeforcesCourse) : DialogWrapper(false) {
  private val textLanguageComboBox: ComboBox<TaskTextLanguage> = ComboBox()
  private val languageComboBox: ComboBox<String> = ComboBox()
  private val doNotShowLanguageDialogCheckBox: JCheckBox = JCheckBox(EduCoreBundle.message("codeforces.prefer.selected.languages"))
  private val comboBoxesWidth: Int = JBUI.scale(130)

  init {
    title = EduCoreBundle.message("dialog.title.codeforces.contest.settings")
    initTextLanguageComboBox()
    initLanguageComboBox()
    init()
  }

  override fun createCenterPanel(): JComponent = panel {
    row {
      label("${EduCoreBundle.message("label.codeforces.contest")}: ${codeforcesCourse.name}")
    }
    row {
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
  }

  override fun getPreferredFocusedComponent(): JComponent = languageComboBox

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
    codeforcesCourse.availableLanguages.sorted().forEach {
      languageComboBox.addItem(it)
    }

    val preferableLanguage = CodeforcesSettings.getInstance().preferableLanguage
    if (preferableLanguage != null && preferableLanguage in codeforcesCourse.availableLanguages) {
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