package com.jetbrains.edu.learning.ai.utils

import com.jetbrains.edu.learning.messages.EduPropertiesBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE_NAME = "ai.llm"

object GrazieLlmProfileProvider : EduPropertiesBundle(BUNDLE_NAME) {
  private fun value(@Suppress("SameParameterValue") @PropertyKey(resourceBundle = BUNDLE_NAME) key: String): String =
    valueOrEmpty(key)

  fun getSolutionStepsProfile(): String = value("LLMProfileIDForGeneratingSolutionSteps")

  fun getNextStepTextHintProfile(): String = value("LLMProfileIDForGeneratingNextStepTextHint")

  fun getNextStepCodeHintProfile(): String = value("LLMProfileIDForGeneratingNextStepCodeHint")

  @Suppress("SameReturnValue")
  fun getAutoValidationProfile(): String = "openai-gpt-4"
}