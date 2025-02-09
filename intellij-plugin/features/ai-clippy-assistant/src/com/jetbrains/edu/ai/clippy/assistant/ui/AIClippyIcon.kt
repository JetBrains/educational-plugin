package com.jetbrains.edu.ai.clippy.assistant.ui

import com.jetbrains.edu.ai.clippy.assistant.messages.EduAIClippyAssistantBundle
import org.jetbrains.annotations.Nls
import java.util.function.Supplier
import javax.swing.Icon

@Suppress("unused", "SpellCheckingInspection")
enum class AIClippyIcon(private val visibleName: Supplier<@Nls String>, val icon: Icon) {
  CASINO(EduAIClippyAssistantBundle.lazyMessage("ai.clippy.icon.casino"), EduAiClippyImages.Casino),
  CLIPPY(EduAIClippyAssistantBundle.lazyMessage("ai.clippy.icon.clippy"), EduAiClippyImages.Clippy),
  FROG(EduAIClippyAssistantBundle.lazyMessage("ai.clippy.icon.frog"), EduAiClippyImages.Frog),
  JACQUE_FRESCO(EduAIClippyAssistantBundle.lazyMessage("ai.clippy.icon.jacque.fresco"), EduAiClippyImages.JacqueFresco),
  LEHA(EduAIClippyAssistantBundle.lazyMessage("ai.clippy.icon.leha"), EduAiClippyImages.Leha),
  PRIGOZHIN(EduAIClippyAssistantBundle.lazyMessage("ai.clippy.icon.prigozhin"), EduAiClippyImages.Prigozhin),
  VSE_RAVNO(EduAIClippyAssistantBundle.lazyMessage("ai.clippy.icon.vse.ravno"), EduAiClippyImages.VseRavno);

  override fun toString(): String = visibleName.get()
}