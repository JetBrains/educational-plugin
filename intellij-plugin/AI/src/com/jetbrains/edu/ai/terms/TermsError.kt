package com.jetbrains.edu.ai.terms

import com.jetbrains.edu.ai.error.AIServiceError
import com.jetbrains.edu.ai.messages.BUNDLE
import org.jetbrains.annotations.PropertyKey

enum class TermsError(@PropertyKey(resourceBundle = BUNDLE) override val messageKey: String) : AIServiceError {
  NO_TERMS("ai.terms.course.terms.does.not.exist"),
  TERMS_UNAVAILABLE_FOR_LEGAL_REASON("ai.terms.terms.unavailable.due.to.license.restrictions");
}