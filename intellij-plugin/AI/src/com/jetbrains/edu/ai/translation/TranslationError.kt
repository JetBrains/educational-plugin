package com.jetbrains.edu.ai.translation

import com.jetbrains.edu.ai.error.AIServiceError
import com.jetbrains.edu.ai.messages.BUNDLE
import org.jetbrains.annotations.PropertyKey

enum class TranslationError(@PropertyKey(resourceBundle = BUNDLE) override val messageKey: String) : AIServiceError {
  NO_TRANSLATION("ai.translation.course.translation.does.not.exist"),
  TRANSLATION_UNAVAILABLE_FOR_LEGAL_REASONS("ai.translation.translation.unavailable.due.to.license.restrictions");
}