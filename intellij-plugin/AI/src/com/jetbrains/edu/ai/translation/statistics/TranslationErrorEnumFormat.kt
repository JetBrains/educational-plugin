package com.jetbrains.edu.ai.translation.statistics

import com.jetbrains.edu.ai.error.AIServiceError
import com.jetbrains.edu.ai.error.CommonAIServiceError
import com.jetbrains.edu.ai.translation.TranslationError

enum class TranslationErrorEnumFormat {
  CONNECTION_ERROR,
  NO_TRANSLATION,
  SERVICE_UNAVAILABLE,
  TRANSLATION_UNAVAILABLE_FOR_LEGAL_REASONS;

  companion object {
    fun from(error: AIServiceError): TranslationErrorEnumFormat = when (error) {
      is CommonAIServiceError -> when (error) {
        CommonAIServiceError.CONNECTION_ERROR -> CONNECTION_ERROR
        CommonAIServiceError.SERVICE_UNAVAILABLE -> SERVICE_UNAVAILABLE
      }
      is TranslationError -> when (error) {
        TranslationError.NO_TRANSLATION -> NO_TRANSLATION
        TranslationError.TRANSLATION_UNAVAILABLE_FOR_LEGAL_REASONS -> TRANSLATION_UNAVAILABLE_FOR_LEGAL_REASONS
      }
      else -> error("Unexpected error type for translation: $error")
    }
  }
}