package com.jetbrains.edu.ai.translation.settings

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.toNullableProperty
import com.intellij.util.application
import com.jetbrains.edu.ai.messages.EduAIBundle
import com.jetbrains.edu.ai.settings.AIOptionsProvider
import com.jetbrains.educational.core.enum.TranslationLanguage
import javax.swing.DefaultComboBoxModel

class TranslationOptions : BoundConfigurable(EduAIBundle.message("settings.ai.translation.display.name")), AIOptionsProvider {
  override fun createPanel(): DialogPanel = panel {
    val settings = application.translationSettings()

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

  private inner class LanguageComboBoxModel : DefaultComboBoxModel<TranslationLanguage>() {
    init {
      @OptIn(ExperimentalStdlibApi::class)
      val languages = TranslationLanguage.entries
        .sortedBy { it.label }
      addAll(languages)
      selectedItem = EduAIBundle.message("ai.translation.choose.language")
    }
  }
}