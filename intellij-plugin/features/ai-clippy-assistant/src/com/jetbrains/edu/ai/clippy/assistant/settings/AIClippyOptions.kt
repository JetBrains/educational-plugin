package com.jetbrains.edu.ai.clippy.assistant.settings

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindIntValue
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.edu.ai.clippy.assistant.messages.EduAIClippyAssistantBundle
import com.jetbrains.edu.ai.settings.AIOptionsProvider

class AIClippyOptions : BoundConfigurable(EduAIClippyAssistantBundle.message("settings.ai.clippy.assistant")), AIOptionsProvider {
  private val settings = AIClippySettings.getInstance()

  private var tone: Int = settings.tone
  private var emotionalIntensity: Int = settings.emotionalIntensity
  private var mistakesAttention: Int = settings.mistakesAttention
  private var communicationStyle: Int = settings.communicationStyle
  private var emojiUsage: Int = settings.emojiUsage
  private var aggression: Int = settings.aggression

  override fun createPanel(): DialogPanel= panel {
    group(displayName) {
      row(EduAIClippyAssistantBundle.message("settings.ai.clippy.tone")) {
        spinner(1..10)
          .bindIntValue(::tone)
          .comment(EduAIClippyAssistantBundle.message("settings.ai.clippy.tone.description"))
      }
      row(EduAIClippyAssistantBundle.message("settings.ai.clippy.emotional.intensity")) {
        spinner(1..10)
          .bindIntValue(::emotionalIntensity)
          .comment(EduAIClippyAssistantBundle.message("settings.ai.clippy.emotional.intensity.description"))
      }
      row(EduAIClippyAssistantBundle.message("settings.ai.clippy.mistakes.attention")) {
        spinner(1..10)
          .bindIntValue(::mistakesAttention)
          .comment(EduAIClippyAssistantBundle.message("settings.ai.clippy.mistakes.attention.description"))
      }
      row(EduAIClippyAssistantBundle.message("settings.ai.clippy.communication.style")) {
        spinner(1..10)
          .bindIntValue(::communicationStyle)
          .comment(EduAIClippyAssistantBundle.message("settings.ai.clippy.communication.style.description"))
      }
      row(EduAIClippyAssistantBundle.message("settings.ai.clippy.emoji.usage")) {
        spinner(1..10)
          .bindIntValue(::emojiUsage)
          .comment(EduAIClippyAssistantBundle.message("settings.ai.clippy.emoji.usage.description"))
      }
      row(EduAIClippyAssistantBundle.message("settings.ai.clippy.aggression")) {
        spinner(1..10)
          .bindIntValue(::aggression)
          .comment(EduAIClippyAssistantBundle.message("settings.ai.clippy.aggression.description"))
      }
    }
  }

  override fun apply() {
    super.apply()
    val aiClippyProperties = AIClippyProperties(
      tone = tone,
      emotionalIntensity = emotionalIntensity,
      mistakesAttention = mistakesAttention,
      communicationStyle = communicationStyle,
      emojiUsage = emojiUsage,
      aggression = aggression,
    )
    AIClippySettings.getInstance().setClippySettings(aiClippyProperties)
  }
}