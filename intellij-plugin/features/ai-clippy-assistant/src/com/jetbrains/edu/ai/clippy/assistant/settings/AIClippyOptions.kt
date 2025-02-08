package com.jetbrains.edu.ai.clippy.assistant.settings

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindIntValue
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.toNullableProperty
import com.intellij.util.IconUtil
import com.jetbrains.edu.ai.clippy.assistant.messages.EduAIClippyAssistantBundle
import com.jetbrains.edu.ai.clippy.assistant.ui.AIClippyIcon
import com.jetbrains.edu.ai.messages.EduAIBundle
import com.jetbrains.edu.ai.settings.AIOptionsProvider
import com.jetbrains.edu.ai.translation.ui.TranslationLanguageComboBoxModel
import com.jetbrains.educational.core.format.enum.TranslationLanguage
import java.awt.Component
import javax.swing.DefaultListCellRenderer
import javax.swing.JList

class AIClippyOptions : BoundConfigurable(EduAIClippyAssistantBundle.message("settings.ai.clippy.assistant")), AIOptionsProvider {
  private val settings = AIClippySettings.getInstance()

  private var clippyIcon: AIClippyIcon = settings.clippyIcon
  private var language: TranslationLanguage = settings.language

  private var aggression: Int = settings.aggression
  private var communicationStyle: Int = settings.communicationStyle
  private var emojiUsage: Int = settings.emojiUsage
  private var emotionalIntensity: Int = settings.emotionalIntensity
  private var humiliation: Int = settings.humiliation
  private var mistakesAttention: Int = settings.mistakesAttention

  override fun createPanel(): DialogPanel= panel {
    group(displayName) {
      row(EduAIClippyAssistantBundle.message("settings.ai.clippy.icon")) {
        comboBox(AIClippyIcon.values().toList(), IconComboBoxCellRenderer())
          .bindItem(::clippyIcon.toNullableProperty())
      }
      row(EduAIBundle.message("settings.ai.translation.preferred.language")) {
        comboBox(TranslationLanguageComboBoxModel())
          .bindItem(::language.toNullableProperty())
      }
      row(EduAIClippyAssistantBundle.message("settings.ai.clippy.aggression")) {
        spinner(1..10)
          .bindIntValue(::aggression)
          .comment(EduAIClippyAssistantBundle.message("settings.ai.clippy.aggression.description"))
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
      row(EduAIClippyAssistantBundle.message("settings.ai.clippy.emotional.intensity")) {
        spinner(1..10)
          .bindIntValue(::emotionalIntensity)
          .comment(EduAIClippyAssistantBundle.message("settings.ai.clippy.emotional.intensity.description"))
      }
      row(EduAIClippyAssistantBundle.message("settings.ai.clippy.humiliation")) {
        spinner(1..10)
          .bindIntValue(::humiliation)
          .comment(EduAIClippyAssistantBundle.message("settings.ai.clippy.humiliation.description"))
      }
      row(EduAIClippyAssistantBundle.message("settings.ai.clippy.mistakes.attention")) {
        spinner(1..10)
          .bindIntValue(::mistakesAttention)
          .comment(EduAIClippyAssistantBundle.message("settings.ai.clippy.mistakes.attention.description"))
      }
    }
  }

  private class IconComboBoxCellRenderer : DefaultListCellRenderer() {
    override fun getListCellRendererComponent(
      list: JList<*>?,
      value: Any?,
      index: Int,
      isSelected: Boolean,
      cellHasFocus: Boolean
    ): Component {
      val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
      if (value is AIClippyIcon) {
        val defaultIcon = IconUtil.scale(AIClippyIcon.CLIPPY.icon, component, 0.1f)
        icon = IconUtil.scaleByIconWidth(value.icon, component, defaultIcon)
        text = value.toString()
      }
      return component
    }
  }

  override fun apply() {
    super.apply()
    val aiClippyProperties = AIClippyProperties(
      icon = clippyIcon,
      language = language,
      aggression = aggression,
      communicationStyle = communicationStyle,
      emojiUsage = emojiUsage,
      emotionalIntensity = emotionalIntensity,
      humiliation = humiliation,
      mistakesAttention = mistakesAttention,
    )
    AIClippySettings.getInstance().setClippySettings(aiClippyProperties)
  }
}