package com.jetbrains.edu.ai.learner.feedback.grazie

import com.jetbrains.edu.ai.clippy.assistant.settings.AIClippyProperties
import com.jetbrains.edu.ai.learner.feedback.prompts.AIFeedbackPromptProvider
import com.jetbrains.educational.ml.core.grazie.GrazieClient
import com.jetbrains.educational.ml.core.grazie.SupportedLLMProfile

private const val LLM_PROMPT_ID: String = "EDU_AI_LEARNER_FEEDBACK_PROMPT_ID"

object AILearnerFeedbackGrazieClient : GrazieClient(LLM_PROMPT_ID, SupportedLLMProfile.GPT4o) {
  suspend fun generateFeedback(clippyProperties: AIClippyProperties, positive: Boolean): String {
    val userPrompt = AIFeedbackPromptProvider.buildUserPrompt(clippyProperties, positive)
    val systemPrompt = AIFeedbackPromptProvider.buildSystemPrompt()
    return grazie.chat(systemPrompt = systemPrompt, userPrompt = userPrompt, temp = 1.0)
  }
}