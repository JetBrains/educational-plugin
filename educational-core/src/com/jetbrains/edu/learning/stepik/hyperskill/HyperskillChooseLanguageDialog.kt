package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.layout.*
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.messages.EduCoreBundle
import javax.swing.JComponent

class HyperskillChooseLanguageDialog : DialogWrapper(false) {
  private val languageComboBox: ComboBox<HyperskillLanguages> = ComboBox()

  private val supportedLanguages: Collection<HyperskillLanguages> by lazy {
    HyperskillLanguages.getAvailableLanguages().sortedBy { it.name }
  }

  init {
    title = EduCoreBundle.message("hyperskill.action.select.language.title")
    isResizable = false
    initLanguageComboBox()
    init()
  }

  override fun createCenterPanel(): JComponent = panel {
    row("${EduCoreBundle.message("label.language")}:") {
      languageComboBox()
    }
  }

  fun selectedLanguage(): HyperskillLanguages = languageComboBox.selectedItem as HyperskillLanguages

  fun areLanguagesAvailable(): Boolean = supportedLanguages.isNotEmpty()

  private fun initLanguageComboBox() {
    supportedLanguages.forEach {
      languageComboBox.addItem(it)
    }
    languageComboBox.setMinimumAndPreferredWidth(JBUI.scale(250))
  }
}