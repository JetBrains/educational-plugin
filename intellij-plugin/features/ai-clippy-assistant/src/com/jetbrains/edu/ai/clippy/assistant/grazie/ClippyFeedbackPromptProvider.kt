package com.jetbrains.edu.ai.clippy.assistant.grazie

import com.jetbrains.edu.ai.clippy.assistant.settings.AIClippyProperties
import com.jetbrains.educational.ml.core.prompt.PromptProvider

object ClippyFeedbackPromptProvider : PromptProvider() {
  fun buildSystemPrompt(): String = ClippyPromptTemplate.FEEDBACK_SYSTEM_PROMPT.process()

  fun buildUserPrompt(clippyProperties: AIClippyProperties): String = ClippyPromptTemplate.FEEDBACK_USER_PROMPT.process(clippyProperties)
}