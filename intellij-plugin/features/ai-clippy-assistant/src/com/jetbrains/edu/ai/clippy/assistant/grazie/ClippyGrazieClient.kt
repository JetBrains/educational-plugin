package com.jetbrains.edu.ai.clippy.assistant.grazie

import com.jetbrains.edu.ai.clippy.assistant.prompts.feedback.ClippyFeedbackPromptProvider
import com.jetbrains.edu.ai.clippy.assistant.prompts.refactoring.ClippyRefactoringPromptProvider
import com.jetbrains.edu.ai.clippy.assistant.settings.AIClippyProperties
import com.jetbrains.educational.ml.core.grazie.GrazieClient
import com.jetbrains.educational.ml.core.grazie.SupportedLLMProfile

private const val LLM_PROMPT_ID: String = "EDU_AI_CLIPPY_PROMPT_ID"

object ClippyGrazieClient : GrazieClient(LLM_PROMPT_ID, SupportedLLMProfile.GPT4o) {
  suspend fun generateFeedback(clippyProperties: AIClippyProperties): String {
    val userPrompt = ClippyFeedbackPromptProvider.buildUserPrompt(clippyProperties)
    val systemPrompt = ClippyFeedbackPromptProvider.buildSystemPrompt()
    return grazie.chat(systemPrompt = systemPrompt, userPrompt = userPrompt, temp = 1.0)
  }

  suspend fun generateRefactoringPatch(programmingLanguage: String, userCode: String, initialCode: String, taskDescription: String): String {
    val systemPrompt = ClippyRefactoringPromptProvider.buildSystemPrompt(programmingLanguage)
    val userPrompt = ClippyRefactoringPromptProvider.buildUserPrompt(taskDescription, userCode, initialCode)
    return grazie.chat(systemPrompt, userPrompt)
  }
}