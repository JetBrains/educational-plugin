package com.jetbrains.edu.ai.translation.settings

import com.intellij.openapi.components.*
import com.jetbrains.educational.core.format.enum.TranslationLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@State(name = "TranslationSettings", storages = [Storage(StoragePathMacros.NON_ROAMABLE_FILE, roamingType = RoamingType.LOCAL)])
class TranslationSettings : PersistentStateComponent<TranslationSettings.State> {
  private val _translationSettings = MutableStateFlow<AutoTranslationProperties?>(null)
  val autoTranslationSettings = _translationSettings.asStateFlow()

  val preferableLanguage: TranslationLanguage?
    get() = _translationSettings.value?.language

  val autoTranslate: Boolean
    get() = _translationSettings.value?.autoTranslate ?: false

  fun setAutoTranslationProperties(properties: AutoTranslationProperties) {
    _translationSettings.value = properties
  }

  override fun getState(): State {
    val state = State()
    state.preferableLanguage = preferableLanguage
    state.autoTranslate = autoTranslate
    return state
  }

  override fun loadState(state: State) {
    val language = state.preferableLanguage ?: return
    _translationSettings.value = AutoTranslationProperties(language, state.autoTranslate)
  }

  class State : BaseState() {
    var preferableLanguage by enum<TranslationLanguage>()
    var autoTranslate by property(false)
  }

  companion object {
    fun getInstance(): TranslationSettings = service()
  }
}