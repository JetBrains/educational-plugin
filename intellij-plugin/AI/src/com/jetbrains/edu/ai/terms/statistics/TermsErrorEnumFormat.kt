package com.jetbrains.edu.ai.terms.statistics

import com.jetbrains.edu.ai.error.AIServiceError
import com.jetbrains.edu.ai.error.CommonAIServiceError
import com.jetbrains.edu.ai.terms.TermsError

enum class TermsErrorEnumFormat {
  CONNECTION_ERROR,
  LANGUAGE_NOT_SUPPORTED,
  NO_TERMS,
  SERVICE_UNAVAILABLE,
  TERMS_UNAVAILABLE_FOR_LEGAL_REASONS;

  companion object {
    fun from(error: AIServiceError): TermsErrorEnumFormat = when (error) {
      is CommonAIServiceError -> when (error) {
        CommonAIServiceError.CONNECTION_ERROR -> CONNECTION_ERROR
        CommonAIServiceError.SERVICE_UNAVAILABLE -> SERVICE_UNAVAILABLE
      }
      is TermsError -> when (error) {
        TermsError.NO_TERMS -> NO_TERMS
        TermsError.TERMS_UNAVAILABLE_FOR_LEGAL_REASON -> TERMS_UNAVAILABLE_FOR_LEGAL_REASONS
        TermsError.LANGUAGE_NOT_SUPPORTED -> LANGUAGE_NOT_SUPPORTED
      }
      else -> error("Unexpected error type for theory lookup: $error")
    }
  }
}