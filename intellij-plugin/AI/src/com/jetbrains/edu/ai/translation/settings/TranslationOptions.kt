package com.jetbrains.edu.ai.translation.settings

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.toNullableProperty
import com.jetbrains.edu.ai.messages.EduAIBundle
import com.jetbrains.edu.ai.settings.AIOptionsProvider
import com.jetbrains.edu.ai.translation.defaultLanguage
import com.jetbrains.educational.translation.enum.Language
import javax.swing.DefaultComboBoxModel

class TranslationOptions : BoundConfigurable(EduAIBundle.message("settings.ai.translation.display.name")), AIOptionsProvider {
  override fun createPanel(): DialogPanel = panel {
    val settings = TranslationSettings.getInstance()

    group(displayName) {
      row(EduAIBundle.message("settings.ai.translation.preferred.language")) {
        comboBox(LanguageComboBoxModel())
          .bindItem(settings::preferableLanguage.toNullableProperty())
      }
      row {
        checkBox(EduAIBundle.message("settings.ai.auto.translate"))
          .bindSelected(settings::autoTranslate)
      }
    }
  }

  private inner class LanguageComboBoxModel : DefaultComboBoxModel<Language>() {
    init {
      addElement(defaultLanguage)

      @OptIn(ExperimentalStdlibApi::class)
      val languages = Language.entries
        .filter { it != defaultLanguage }
        .sortedBy { it.label }
      addAll(languages)

      selectedItem = defaultLanguage
    }
  }
}