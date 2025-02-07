package com.jetbrains.edu.ai.learner.feedback.prompts

import com.jetbrains.edu.ai.clippy.assistant.settings.AIClippyProperties
import com.jetbrains.educational.ml.core.prompt.PromptProvider

object AIFeedbackPromptProvider : PromptProvider() {
  fun buildSystemPrompt(): String = AIFeedbackPromptTemplate.FEEDBACK_SYSTEM_PROMPT.process()

  fun buildUserPrompt(clippyProperties: AIClippyProperties, successful: Boolean): String =
    if (successful) {
      AIFeedbackPromptTemplate.FEEDBACK_POSITIVE_USER_PROMPT.process(clippyProperties)
    }
    else {
      AIFeedbackPromptTemplate.FEEDBACK_NEGATIVE_USER_PROMPT.process(clippyProperties)
    }
}