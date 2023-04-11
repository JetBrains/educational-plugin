package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.EduLanguage
import com.jetbrains.edu.learning.configuration.EduConfiguratorManager
import com.jetbrains.edu.learning.messages.EduCoreBundle
import javax.swing.JComponent

class HyperskillChooseLanguageDialog : DialogWrapper(false) {
  private val languageComboBox: ComboBox<EduLanguage> = ComboBox()

  private val supportedLanguages: Collection<EduLanguage> by lazy {
    EduConfiguratorManager.allExtensions()
      .filter { it.courseType == HYPERSKILL_TYPE }
      .mapTo(HashSet()) { EduLanguage.get(it.language) }
  }

  init {
    title = EduCoreBundle.message("hyperskill.action.select.language.title")
    isResizable = false
    initLanguageComboBox()
    init()
  }

  override fun createCenterPanel(): JComponent = panel {
    row("${EduCoreBundle.message("label.language")}:") {
      cell(languageComboBox)
    }
  }

  fun selectedLanguage(): EduLanguage = languageComboBox.selectedItem as EduLanguage

  fun areLanguagesAvailable(): Boolean = supportedLanguages.isNotEmpty()

  private fun initLanguageComboBox() {
    supportedLanguages
      .sortedBy { it.toString() }
      .forEach { languageComboBox.addItem(it) }
    languageComboBox.setMinimumAndPreferredWidth(JBUI.scale(250))
  }
}