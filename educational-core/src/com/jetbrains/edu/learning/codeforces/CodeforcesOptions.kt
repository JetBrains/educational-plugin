package com.jetbrains.edu.learning.codeforces

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.layout.*
import com.jetbrains.edu.coursecreator.getDefaultLanguageId
import com.jetbrains.edu.learning.settings.OptionsProvider
import javax.swing.JComponent

class CodeforcesOptions : OptionsProvider {
  private val textLanguageComboBox: ComboBox<TaskTextLanguage> = ComboBox()
  private val languageComboBox: ComboBox<String> = ComboBox()
  private val state: State

  init {
    initTextLanguageComboBox()
    initLanguageComboBox()
    state = State(getTextLanguage(), getLanguage())
  }

  data class State(var textLanguage: String, var language: String)

  private fun initTextLanguageComboBox() {
    TaskTextLanguage.values().forEach {
      textLanguageComboBox.addItem(it)
    }

    val preferableTextLanguage = CodeforcesSettings.getInstance().codeforcesPreferableTextLanguage
    if (preferableTextLanguage != null) {
      textLanguageComboBox.selectedItem = TaskTextLanguage.valueOf(preferableTextLanguage)
    }
  }

  private fun initLanguageComboBox() {
    CodeforcesLanguageProvider.getSupportedLanguages()
      .sorted()
      .forEach {
        languageComboBox.addItem(it)
      }

    val preferableLanguage = CodeforcesSettings.getInstance().codeforcesPreferableLanguage
    if (preferableLanguage != null) {
      languageComboBox.selectedItem = preferableLanguage
    }
    else {
      val defaultLanguageId = getDefaultLanguageId()
      if (defaultLanguageId != null) {
        languageComboBox.selectedItem = CodeforcesLanguageProvider.getPreferableCodeforcesLanguage(defaultLanguageId)
      }
    }
  }

  override fun getDisplayName(): String = "Codeforces"

  override fun apply() {
    val textLanguage = getTextLanguage()
    val language = getLanguage()

    val codeforcesSettings = CodeforcesSettings.getInstance()
    codeforcesSettings.codeforcesPreferableTextLanguage = textLanguage
    codeforcesSettings.codeforcesPreferableLanguage = language

    state.textLanguage = textLanguage
    state.language = language
  }

  override fun createComponent(): JComponent? = panel {
    row("Language:") {
      textLanguageComboBox()
    }
    row("Programming language:") {
      languageComboBox()
    }
  }

  override fun reset() {
    textLanguageComboBox.selectedItem = TaskTextLanguage.valueOf(state.textLanguage)
    languageComboBox.selectedItem = state.language
  }

  override fun isModified(): Boolean = state.textLanguage != getTextLanguage() || state.language != getLanguage()

  private fun getTextLanguage() = (textLanguageComboBox.selectedItem as TaskTextLanguage).name

  private fun getLanguage() = languageComboBox.selectedItem!!.toString()
}