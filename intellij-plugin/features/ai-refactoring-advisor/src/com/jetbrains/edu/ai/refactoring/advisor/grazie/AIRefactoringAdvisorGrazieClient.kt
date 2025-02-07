package com.jetbrains.edu.ai.refactoring.advisor.grazie

import com.jetbrains.edu.ai.refactoring.advisor.prompts.AIRefactoringContext
import com.jetbrains.edu.ai.refactoring.advisor.prompts.AIRefactoringPromptProvider
import com.jetbrains.educational.ml.core.grazie.GrazieClient
import com.jetbrains.educational.ml.core.grazie.SupportedLLMProfile

private const val LLM_PROMPT_ID: String = "EDU_AI_REFACTORING_PROMPT_ID"

object AIRefactoringAdvisorGrazieClient : GrazieClient(LLM_PROMPT_ID, SupportedLLMProfile.GPT4o) {
  suspend fun generateRefactoringPatch(refactoringContext: AIRefactoringContext): String {
    val systemPrompt = AIRefactoringPromptProvider.buildSystemPrompt(refactoringContext)
    val userPrompt = AIRefactoringPromptProvider.buildUserPrompt(refactoringContext)
    return grazie.chat(systemPrompt, userPrompt)
  }
}