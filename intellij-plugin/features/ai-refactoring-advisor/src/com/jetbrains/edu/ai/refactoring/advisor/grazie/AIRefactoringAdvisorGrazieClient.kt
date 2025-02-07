package com.jetbrains.edu.ai.refactoring.advisor.grazie

import com.jetbrains.edu.ai.refactoring.advisor.prompts.AIRefactoringUserContext
import com.jetbrains.edu.ai.refactoring.advisor.prompts.AIRefactoringPromptProvider
import com.jetbrains.edu.ai.refactoring.advisor.prompts.AIRefactoringSystemContext
import com.jetbrains.educational.ml.core.grazie.GrazieClient
import com.jetbrains.educational.ml.core.grazie.SupportedLLMProfile

private const val LLM_PROMPT_ID: String = "EDU_AI_REFACTORING_PROMPT_ID"

object AIRefactoringAdvisorGrazieClient : GrazieClient(LLM_PROMPT_ID, SupportedLLMProfile.GPT4o) {
  suspend fun generateRefactoringPatch(userContext: AIRefactoringUserContext, systemContext: AIRefactoringSystemContext): String {
    val systemPrompt = AIRefactoringPromptProvider.buildSystemPrompt(systemContext)
    val userPrompt = AIRefactoringPromptProvider.buildUserPrompt(userContext)
    return grazie.chat(systemPrompt, userPrompt)
  }
}