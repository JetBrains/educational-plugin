package com.jetbrains.edu.ai.error.explanation.grazie

import com.jetbrains.edu.ai.error.explanation.prompts.ErrorExplanationContext
import com.jetbrains.edu.ai.error.explanation.prompts.ErrorExplanationPromptProvider
import com.jetbrains.educational.ml.core.grazie.GrazieClient
import com.jetbrains.educational.ml.core.grazie.SupportedLLMProfile

private const val LLM_PROMPT_ID: String = "EDU_AI_ERROR_EXPLANATION_PROMPT_ID"

object ErrorExplanationGrazieClient : GrazieClient(LLM_PROMPT_ID, SupportedLLMProfile.GPT4o) {
  suspend fun getErrorExplanation(context: ErrorExplanationContext): String {
    val systemPrompt = ErrorExplanationPromptProvider.getSystemTemplate()
    val userPrompt = ErrorExplanationPromptProvider.getUserTemplate(context)
    return grazie.chat(systemPrompt, userPrompt)
  }
}