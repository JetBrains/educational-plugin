package com.jetbrains.edu.ai.clippy.assistant.prompts

import com.jetbrains.edu.ai.clippy.assistant.settings.AIClippyProperties
import com.jetbrains.educational.ml.core.prompt.PromptProvider

object ClippyFeedbackPromptProvider : PromptProvider() {
  fun buildSystemPrompt(): String = ClippyFeedbackPromptTemplate.FEEDBACK_SYSTEM_PROMPT.process()

  fun buildUserPrompt(clippyProperties: AIClippyProperties): String =
    ClippyFeedbackPromptTemplate.FEEDBACK_USER_PROMPT.process(clippyProperties)
}