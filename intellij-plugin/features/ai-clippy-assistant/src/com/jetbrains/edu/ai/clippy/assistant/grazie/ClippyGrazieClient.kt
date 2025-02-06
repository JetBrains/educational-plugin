package com.jetbrains.edu.ai.clippy.assistant.grazie

import com.jetbrains.educational.ml.core.grazie.GrazieClient
import com.jetbrains.educational.ml.core.grazie.SupportedLLMProfile

const val CLIPPY_PROMPT_ID: String = "educational-clippy-prompt-v6"

object ClippyGrazieClient : GrazieClient(CLIPPY_PROMPT_ID, SupportedLLMProfile.GPT4o) {
  suspend fun getClippySuggestions(systemPrompt: String, userPrompt: String) = grazie.chat(systemPrompt, userPrompt)
}