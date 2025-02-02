package com.jetbrains.edu.ai.error

import com.jetbrains.edu.ai.messages.BUNDLE
import org.jetbrains.annotations.PropertyKey

enum class CommonAIServiceError(@PropertyKey(resourceBundle = BUNDLE) override val messageKey: String) : AIServiceError {
  CONNECTION_ERROR("ai.service.could.not.connect"),
  SERVICE_UNAVAILABLE("ai.service.is.currently.unavailable");
}