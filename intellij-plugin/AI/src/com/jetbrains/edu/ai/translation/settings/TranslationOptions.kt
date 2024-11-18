package com.jetbrains.edu.ai.translation.settings

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.*
import com.intellij.util.application
import com.jetbrains.edu.ai.messages.EduAIBundle
import com.jetbrains.edu.ai.settings.AIOptionsProvider
import com.jetbrains.educational.core.format.enum.TranslationLanguage
import javax.swing.DefaultComboBoxModel

class TranslationOptions : BoundConfigurable(EduAIBundle.message("settings.ai.translation.display.name")), AIOptionsProvider {
  private val settings = application.translationSettings()
  private var autoTranslate: Boolean = settings.autoTranslate
  private var preferableLanguage: TranslationLanguage? = settings.preferableLanguage
  private lateinit var checkBox: Cell<JBCheckBox>

  override fun createPanel(): DialogPanel = panel {
    group(displayName) {
      row(EduAIBundle.message("settings.ai.translation.preferred.language")) {
        comboBox(LanguageComboBoxModel())
          .bindItem(::preferableLanguage.toNullableProperty())
          .onChanged {
            checkBox.enabled(it.selectedItem != null)
          }
      }
      row {
        checkBox = checkBox(EduAIBundle.message("settings.ai.auto.translate"))
          .enabled(preferableLanguage != null)
          .bindSelected(::autoTranslate)
      }
    }
  }

  override fun apply() {
    super.apply()
    val language = preferableLanguage ?: return
    val autoTranslationProperties = AutoTranslationProperties(language, autoTranslate)
    application.translationSettings().setAutoTranslationProperties(autoTranslationProperties)
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