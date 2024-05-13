package com.jetbrains.edu.learning.eduAssistant.grazie

import ai.grazie.api.gateway.client.api.llm.ChatRequestBuilder
import ai.grazie.model.llm.profile.GoogleProfileIDs
import ai.grazie.model.llm.profile.GrazieLLMProfileIDs
import ai.grazie.model.llm.profile.LLMProfileID
import ai.grazie.model.llm.profile.OpenAIProfileIDs
import ai.grazie.model.llm.prompt.LLMPromptID
import com.jetbrains.edu.learning.ai.utils.GrazieLlmProfileProvider

enum class GenerationContextProfile(private val llmProfileId: String) {
  NEXT_STEP_TEXT_HINT(GrazieLlmProfileProvider.getNextStepTextHintProfile()),
  NEXT_STEP_CODE_HINT(GrazieLlmProfileProvider.getNextStepCodeHintProfile()),
  AUTO_VALIDATION(GrazieLlmProfileProvider.getAutoValidationProfile());

  private fun getProfileById() =
    when(llmProfileId) {
      "openai-chat-gpt" -> OpenAIProfileIDs.Chat.ChatGPT
      "openai-gpt-4" -> OpenAIProfileIDs.Chat.GPT4
      "google-chat-bison" -> GoogleProfileIDs.Chat.Bison
      "grazie-chat-llama-v2-13b" -> GrazieLLMProfileIDs.LLAMA.Medium
      "gemini-pro" -> GoogleProfileIDs.Chat.GeminiPro
      "gemini-ultra" -> GoogleProfileIDs.Chat.GeminiUltra
      "gemini-pro-1.5" -> GoogleProfileIDs.Chat.GeminiPro1_5
      else -> error("Unsupported LLM Profile ID: $llmProfileId")
    }

  private fun LLMProfileID.isTemperatureApplicable() = this == OpenAIProfileIDs.Chat.ChatGPT || this == OpenAIProfileIDs.Chat.GPT4

  fun buildChatRequest(builder: ChatRequestBuilder, systemPrompt: String?, userPrompt: String, temp: Double) {
    builder.prompt = LLMPromptID("learning-assistant-prompt")
    builder.profile = getProfileById()
    if (builder.profile?.isTemperatureApplicable() == true) {
      builder.temperature = temp
    }
    builder.messages {
      systemPrompt?.let { system(it) }
      user(userPrompt)
    }
  }
}
