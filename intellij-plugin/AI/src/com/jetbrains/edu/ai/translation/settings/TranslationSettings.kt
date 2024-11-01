package com.jetbrains.edu.ai.translation.settings

import com.intellij.openapi.application.Application
import com.intellij.openapi.components.*
import com.jetbrains.educational.core.enum.TranslationLanguage

fun Application.translationSettings(): TranslationSettings = service()

@Service
@State(name = "TranslationSettings", storages = [Storage(StoragePathMacros.NON_ROAMABLE_FILE, roamingType = RoamingType.LOCAL)])
class TranslationSettings : SimplePersistentStateComponent<TranslationSettings.State>(State()) {
  var autoTranslate: Boolean
    get() = state.autoTranslate
    set(value) {
      state.autoTranslate = value
    }

  var preferableLanguage: TranslationLanguage?
    get() = state.preferableLanguage
    set(value) {
      state.preferableLanguage = value
    }

  class State : BaseState() {
    var preferableLanguage by enum<TranslationLanguage>()
    var autoTranslate by property(false)
  }
}