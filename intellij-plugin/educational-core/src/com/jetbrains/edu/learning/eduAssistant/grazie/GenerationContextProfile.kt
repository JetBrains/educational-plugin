package com.jetbrains.edu.learning.eduAssistant.grazie

import ai.grazie.model.llm.profile.GoogleProfileIDs
import ai.grazie.model.llm.profile.GrazieLLMProfileIDs
import ai.grazie.model.llm.profile.OpenAIProfileIDs

enum class GenerationContextProfile(private val llmProfileId: String) {
  SOLUTION_STEPS(System.getProperty("llm.profile.id.for.generating.solution.steps")),
  NEXT_STEP_TEXT_HINT(System.getProperty("llm.profile.id.for.generating.next.step.text.hint")),
  NEXT_STEP_CODE_HINT(System.getProperty("llm.profile.id.for.generating.next.step.code.hint")),
  VALIDATION("openai-gpt-4");

  fun getProfileById() =
    when(this.llmProfileId) {
      "openai-chat-gpt" -> OpenAIProfileIDs.Chat.ChatGPT
      "openai-gpt-4" -> OpenAIProfileIDs.Chat.GPT4
      "google-chat-bison" -> GoogleProfileIDs.Chat.Bison
      "grazie-chat-llama-v2-13b" -> GrazieLLMProfileIDs.LLAMA.Medium
      else -> throw IllegalArgumentException("Unsupported LLM Profile ID: ${this.llmProfileId}")
    }
}
