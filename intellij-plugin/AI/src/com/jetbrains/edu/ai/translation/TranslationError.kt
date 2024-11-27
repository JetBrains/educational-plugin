package com.jetbrains.edu.ai.translation

import com.jetbrains.edu.ai.messages.BUNDLE
import com.jetbrains.edu.ai.messages.EduAIBundle
import com.jetbrains.edu.learning.Err
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

enum class TranslationError(@PropertyKey(resourceBundle = BUNDLE) val messageKey: String) {
  CONNECTION_ERROR("ai.translation.service.could.not.connect"),
  NO_TRANSLATION("ai.translation.course.translation.does.not.exist"),
  SERVICE_UNAVAILABLE("ai.translation.service.is.currently.unavailable"),
  TRANSLATION_UNAVAILABLE_FOR_LEGAL_REASONS("ai.translation.translation.unavailable.due.to.license.restrictions");

  fun asErr(): Err<TranslationError> = Err(this)

  fun message(): @NonNls String = EduAIBundle.message(messageKey)
}