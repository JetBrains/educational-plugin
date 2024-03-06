package com.jetbrains.edu.learning.eduAssistant.grazie

import ai.grazie.model.llm.profile.GoogleProfileIDs
import ai.grazie.model.llm.profile.GrazieLLMProfileIDs
import ai.grazie.model.llm.profile.OpenAIProfileIDs
import com.jetbrains.edu.learning.ai.utils.GrazieLlmProfileProvider

enum class GenerationContextProfile(private val llmProfileId: String) {
  SOLUTION_STEPS(GrazieLlmProfileProvider.getSolutionStepsProfile()),
  NEXT_STEP_TEXT_HINT(GrazieLlmProfileProvider.getNextStepTextHintProfile()),
  NEXT_STEP_CODE_HINT(GrazieLlmProfileProvider.getNextStepCodeHintProfile()),
  AUTO_VALIDATION(GrazieLlmProfileProvider.getAutoValidationProfile());

  fun getProfileById() =
    when(llmProfileId) {
      "openai-chat-gpt" -> OpenAIProfileIDs.Chat.ChatGPT
      "openai-gpt-4" -> OpenAIProfileIDs.Chat.GPT4
      "google-chat-bison" -> GoogleProfileIDs.Chat.Bison
      "grazie-chat-llama-v2-13b" -> GrazieLLMProfileIDs.LLAMA.Medium
      else -> error("Unsupported LLM Profile ID: $llmProfileId")
    }
}
