package com.jetbrains.edu.learning.codeforces

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.edu.coursecreator.getDefaultLanguageId
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.codeforces.CodeforcesCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseBindData
import com.jetbrains.edu.learning.newproject.ui.courseSettings.CourseSettingsPanel
import com.jetbrains.edu.learning.newproject.ui.errors.SettingsValidationResult
import java.awt.event.ItemEvent
import javax.swing.JCheckBox
import javax.swing.JComponent

class ChooseCodeforcesContestLanguagesDialog(private val codeforcesCourse: CodeforcesCourse) : DialogWrapper(false) {
  private val textLanguageComboBox: ComboBox<TaskTextLanguage> = ComboBox<TaskTextLanguage>()
  private val languageComboBox: ComboBox<String> = ComboBox<String>()
  private val doNotShowLanguageDialogCheckBox: JCheckBox = JCheckBox(EduCoreBundle.message("codeforces.prefer.selected.languages"))
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

    val settingsValidationResult = courseSettingsPanel.languageSettings?.validate(codeforcesCourse, courseSettingsPanel.locationString)
    if (settingsValidationResult is SettingsValidationResult.Ready) {
      val message = settingsValidationResult.validationMessage?.message
      if (message != null) {
        setErrorText(message)
        return ValidationInfo(message, courseSettingsPanel.settingsPanel)
      }
    }

    return super.doValidate()
  }

  override fun postponeValidation(): Boolean {
    return false
  }

  private fun setLanguage() {
    CodeforcesLanguageProvider.getLanguageIdAndVersion(selectedLanguage()).apply {
      codeforcesCourse.languageId = first
      codeforcesCourse.languageVersion = second
    }
    codeforcesCourse.programTypeId = CodeforcesLanguageProvider.getProgramTypeId(selectedLanguage())
    courseSettingsPanel.onCourseSelectionChanged(CourseBindData(codeforcesCourse))
  }

  override fun createCenterPanel(): JComponent = panel {
    row("${EduCoreBundle.message("label.codeforces.programming.language")}:") {
      cell(languageComboBox)
        .columns(COLUMNS_MEDIUM)
    }
    row("${EduCoreBundle.message("label.codeforces.display.in")}:") {
      cell(textLanguageComboBox)
        .columns(COLUMNS_MEDIUM)
    }
    row {
      cell(courseSettingsPanel).align(AlignX.FILL)
    }
  }

  override fun getPreferredFocusedComponent(): JComponent = languageComboBox

  fun selectedTaskTextLanguage(): TaskTextLanguage = textLanguageComboBox.selectedItem as TaskTextLanguage

  // TODO: it's possible not to have any available language, so `languageComboBox.selectedItem` may be null
  fun selectedLanguage(): String = languageComboBox.selectedItem as String

  fun isDoNotShowLanguageDialog(): Boolean = doNotShowLanguageDialogCheckBox.isSelected

  fun contestLocation(): String? = courseSettingsPanel.locationString

  fun languageSettings(): LanguageSettings<*>? = courseSettingsPanel.languageSettings

  private fun initTextLanguageComboBox() {
    TaskTextLanguage.values().forEach {
      textLanguageComboBox.addItem(it)
    }
  }

  private fun initLanguageComboBox() {
    codeforcesCourse.availableLanguages.sorted().forEach {
      languageComboBox.addItem(it)
    }

    val defaultLanguageId = getDefaultLanguageId()
    if (defaultLanguageId != null) {
      val preferableCodeforcesLanguage = CodeforcesLanguageProvider.getPreferableCodeforcesLanguage(defaultLanguageId)
      // Technically, it's possible to have disabled integration for preferable language.
      // In this case, it doesn't make sense to unselect the current selected item
      if (preferableCodeforcesLanguage != null) {
        languageComboBox.selectedItem = preferableCodeforcesLanguage
      }
    }
  }
}
