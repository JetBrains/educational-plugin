package com.jetbrains.edu.learning.codeforces

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.layout.*
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.coursecreator.getDefaultLanguageId
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseBindData
import com.jetbrains.edu.learning.newproject.ui.courseSettings.CourseSettingsPanel
import java.awt.event.ItemEvent
import javax.swing.JCheckBox
import javax.swing.JComponent

class ChooseCodeforcesContestLanguagesDialog(private val codeforcesCourse: CodeforcesCourse) : DialogWrapper(false) {
  private val textLanguageComboBox: ComboBox<TaskTextLanguage> = ComboBox<TaskTextLanguage>()
  private val languageComboBox: ComboBox<String> = ComboBox<String>()
  private val doNotShowLanguageDialogCheckBox: JCheckBox = JCheckBox(EduCoreBundle.message("codeforces.prefer.selected.languages"))
  private val comboBoxesWidth: Int = JBUI.scale(300)
  private val courseSettingsPanel: CourseSettingsPanel =
    CourseSettingsPanel(disposable, true, EduCoreBundle.message("codeforces.project.settings"))

  init {
    title = EduCoreBundle.message("dialog.title.codeforces.contest.settings")
    initTextLanguageComboBox()
    initLanguageComboBox()
    setOKButtonText(EduCoreBundle.message("course.dialog.start.button.codeforces.start.contest"))

    init()

    setLanguage()

    languageComboBox.addItemListener {
      if (it.stateChange == ItemEvent.SELECTED) {
        setLanguage()
      }
    }
  }

  override fun doValidate(): ValidationInfo? {
    if (courseSettingsPanel.locationString.isNullOrEmpty()) {
      val message = EduCoreBundle.message("error.enter.location")
      setErrorText(message)
      return ValidationInfo(message, courseSettingsPanel.locationField)
    }

    val validationMessage = courseSettingsPanel.languageSettings?.validate(codeforcesCourse, courseSettingsPanel.locationString)
    if (validationMessage != null) {
      val message = validationMessage.message
      setErrorText(message)
      return ValidationInfo(message, courseSettingsPanel.settingsPanel)
    }

    return super.doValidate()
  }

  override fun postponeValidation(): Boolean {
    return false
  }

  private fun setLanguage() {
    codeforcesCourse.programmingLanguage = CodeforcesLanguageProvider.getLanguageIdAndVersion(selectedLanguage())
    codeforcesCourse.programTypeId = CodeforcesLanguageProvider.getProgramTypeId(selectedLanguage())
    courseSettingsPanel.onCourseSelectionChanged(CourseBindData(codeforcesCourse))
  }

  override fun createCenterPanel(): JComponent = panel {
    row("${EduCoreBundle.message("label.codeforces.programming.language")}: ") {
      languageComboBox()
    }
    row("${EduCoreBundle.message("label.codeforces.display.in")}: ") {
      textLanguageComboBox()
    }
    row {
      courseSettingsPanel()
    }
  }

  override fun getPreferredFocusedComponent(): JComponent = languageComboBox

  fun selectedTaskTextLanguage(): TaskTextLanguage = textLanguageComboBox.selectedItem as TaskTextLanguage

  fun selectedLanguage(): String = languageComboBox.selectedItem as String

  fun isDoNotShowLanguageDialog(): Boolean = doNotShowLanguageDialogCheckBox.isSelected

  fun contestLocation(): String? = courseSettingsPanel.locationString

  fun languageSettings(): LanguageSettings<*>? = courseSettingsPanel.languageSettings

  private fun initTextLanguageComboBox() {
    TaskTextLanguage.values().forEach {
      textLanguageComboBox.addItem(it)
    }


    textLanguageComboBox.setMinimumAndPreferredWidth(comboBoxesWidth)
  }

  private fun initLanguageComboBox() {
    codeforcesCourse.availableLanguages.sorted().forEach {
      languageComboBox.addItem(it)
    }

    val defaultLanguageId = getDefaultLanguageId()
    if (defaultLanguageId != null) {
      languageComboBox.selectedItem = CodeforcesLanguageProvider.getPreferableCodeforcesLanguage(defaultLanguageId)
    }

    languageComboBox.setMinimumAndPreferredWidth(comboBoxesWidth)
  }
}
