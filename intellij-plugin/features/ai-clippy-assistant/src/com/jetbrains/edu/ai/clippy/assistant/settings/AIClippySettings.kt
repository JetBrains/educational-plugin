package com.jetbrains.edu.ai.clippy.assistant.settings

import com.intellij.openapi.components.*

@State(name = "AIClippySettings", storages = [Storage(StoragePathMacros.NON_ROAMABLE_FILE, roamingType = RoamingType.LOCAL)])
class AIClippySettings : PersistentStateComponent<AIClippySettings.State> {
  private var clippySettings = AIClippyProperties()

  val tone: Int get() = clippySettings.tone
  val emotionalIntensity: Int get() = clippySettings.emotionalIntensity
  val mistakesAttention: Int get() = clippySettings.mistakesAttention
  val communicationStyle: Int get() = clippySettings.communicationStyle
  val emojiUsage: Int get() = clippySettings.emojiUsage
  val aggression: Int get() = clippySettings.aggression
  val humiliation: Int get() = clippySettings.humiliation

  fun getClippySettings(): AIClippyProperties = clippySettings

  fun setClippySettings(settings: AIClippyProperties) {
    clippySettings = settings
  }

  override fun getState(): State {
    val state = State()
    state.tone = clippySettings.tone
    state.emotionalIntensity = clippySettings.emotionalIntensity
    state.mistakesAttention = clippySettings.mistakesAttention
    state.communicationStyle = clippySettings.communicationStyle
    state.emojiUsage = clippySettings.emojiUsage
    state.aggression = clippySettings.aggression
    state.humiliation = clippySettings.humiliation
    return state
  }

  override fun loadState(state: State) {
    clippySettings = AIClippyProperties(
      tone = state.tone,
      emotionalIntensity = state.emotionalIntensity,
      mistakesAttention = state.mistakesAttention,
      communicationStyle = state.communicationStyle,
      emojiUsage = state.emojiUsage,
      aggression = state.aggression,
      humiliation = state.humiliation,
    )
  }

  class State : BaseState() {
    var tone by property(5)
    var emotionalIntensity by property(5)
    var mistakesAttention by property(5)
    var communicationStyle by property(5)
    var emojiUsage by property(5)
    var aggression by property(5)
    var humiliation by property(5)
  }

  companion object {
    fun getInstance(): AIClippySettings = service()
  }
}