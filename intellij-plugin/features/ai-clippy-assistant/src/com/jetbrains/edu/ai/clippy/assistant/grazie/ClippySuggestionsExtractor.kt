package com.jetbrains.edu.ai.clippy.assistant.grazie

import com.jetbrains.edu.ai.clippy.assistant.prompts.ClippyPromptProvider

object ClippySuggestionsExtractor {
  suspend fun getClippyPatch(programmingLanguage: String, userCode: String, initialCode: String, taskDescription: String): String {
    val systemPrompt = ClippyPromptProvider.buildSystemPrompt(programmingLanguage)
    val userPrompt = ClippyPromptProvider.buildUserPrompt(taskDescription, userCode, initialCode)
    return ClippyGrazieClient.getClippySuggestions(systemPrompt, userPrompt).dropFormatting()
  }

  private fun String.dropFormatting() = split('\n').drop(1).dropLast(1).joinToString("\n")
}