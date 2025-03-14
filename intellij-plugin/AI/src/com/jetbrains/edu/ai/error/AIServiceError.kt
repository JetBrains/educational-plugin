package com.jetbrains.edu.ai.error

import com.jetbrains.edu.ai.messages.EduAIBundle
import com.jetbrains.edu.learning.Err
import org.jetbrains.annotations.NonNls

interface AIServiceError {
  val messageKey: String

  // TODO(drop this method)
  fun asErr(): Err<AIServiceError> = Err(this)

  fun message(): @NonNls String = EduAIBundle.message(messageKey)
}