package com.jetbrains.edu.ai.learner.feedback.prompts

import com.jetbrains.edu.ai.clippy.assistant.settings.AIClippyProperties
import com.jetbrains.educational.ml.core.prompt.PromptProvider

private const val TEMPLATES_FOLDER: String = "/templates/learnerFeedback"

object AIFeedbackPromptProvider : PromptProvider(TEMPLATES_FOLDER) {
  fun buildSystemPrompt(): String = AIFeedbackPromptTemplate.FEEDBACK_SYSTEM_PROMPT.process()

  fun buildUserPrompt(clippyProperties: AIClippyProperties, successful: Boolean): String =
    if (successful) {
      AIFeedbackPromptTemplate.FEEDBACK_POSITIVE_USER_PROMPT.process(clippyProperties)
    }
    else {
      AIFeedbackPromptTemplate.FEEDBACK_NEGATIVE_USER_PROMPT.process(clippyProperties)
    }
}