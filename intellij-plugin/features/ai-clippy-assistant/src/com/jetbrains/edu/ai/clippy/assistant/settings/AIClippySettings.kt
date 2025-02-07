package com.jetbrains.edu.ai.clippy.assistant.settings

import com.intellij.openapi.components.*
import com.jetbrains.educational.core.format.enum.TranslationLanguage

@State(name = "AIClippySettings", storages = [Storage(StoragePathMacros.NON_ROAMABLE_FILE, roamingType = RoamingType.LOCAL)])
class AIClippySettings : PersistentStateComponent<AIClippySettings.State> {
  private var clippySettings = AIClippyProperties()

  val language: TranslationLanguage get() = clippySettings.language

  val aggression: Int get() = clippySettings.aggression
  val communicationStyle: Int get() = clippySettings.communicationStyle
  val emojiUsage: Int get() = clippySettings.emojiUsage
  val emotionalIntensity: Int get() = clippySettings.emotionalIntensity
  val humiliation: Int get() = clippySettings.humiliation
  val mistakesAttention: Int get() = clippySettings.mistakesAttention

  fun getClippySettings(): AIClippyProperties = clippySettings

  fun setClippySettings(settings: AIClippyProperties) {
    clippySettings = settings
  }

  override fun getState(): State {
    val state = State()

    state.aggression = clippySettings.aggression
    state.communicationStyle = clippySettings.communicationStyle
    state.emojiUsage = clippySettings.emojiUsage
    state.emotionalIntensity = clippySettings.emotionalIntensity
    state.humiliation = clippySettings.humiliation
    state.mistakesAttention = clippySettings.mistakesAttention

    state.language = clippySettings.language
    return state
  }

  override fun loadState(state: State) {
    clippySettings = AIClippyProperties(
      language = state.language ?: TranslationLanguage.ENGLISH,
      aggression = state.aggression,
      communicationStyle = state.communicationStyle,
      emojiUsage = state.emojiUsage,
      emotionalIntensity = state.emotionalIntensity,
      humiliation = state.humiliation,
      mistakesAttention = state.mistakesAttention
    )
  }

  class State : BaseState() {
    var aggression by property(5)
    var communicationStyle by property(5)
    var emojiUsage by property(5)
    var emotionalIntensity by property(5)
    var humiliation by property(5)
    var mistakesAttention by property(5)

    var language by enum<TranslationLanguage>()
  }

  companion object {
    fun getInstance(): AIClippySettings = service()
  }
}