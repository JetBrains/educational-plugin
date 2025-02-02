package com.jetbrains.edu.ai.translation.statistics

import com.jetbrains.edu.ai.error.AIServiceError
import com.jetbrains.edu.ai.error.CommonAIServiceError
import com.jetbrains.edu.ai.translation.TranslationError

enum class TranslationErrorEnumFormat {
  CONNECTION_ERROR,
  NO_TRANSLATION,
  SERVICE_UNAVAILABLE,
  TRANSLATION_UNAVAILABLE_FOR_LEGAL_REASONS;
}

fun AIServiceError.toStatisticsFormat(): TranslationErrorEnumFormat = when (this) {
  is CommonAIServiceError -> when(this) {
    CommonAIServiceError.CONNECTION_ERROR -> TranslationErrorEnumFormat.CONNECTION_ERROR
    CommonAIServiceError.SERVICE_UNAVAILABLE -> TranslationErrorEnumFormat.SERVICE_UNAVAILABLE
  }
  is TranslationError -> when(this) {
    TranslationError.NO_TRANSLATION -> TranslationErrorEnumFormat.NO_TRANSLATION
    TranslationError.TRANSLATION_UNAVAILABLE_FOR_LEGAL_REASONS -> TranslationErrorEnumFormat.TRANSLATION_UNAVAILABLE_FOR_LEGAL_REASONS
  }
  else -> error("Unexpected error type for translation: $this")
}
