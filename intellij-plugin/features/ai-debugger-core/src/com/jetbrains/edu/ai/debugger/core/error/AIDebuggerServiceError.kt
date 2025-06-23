package com.jetbrains.edu.ai.debugger.core.error

import com.jetbrains.edu.ai.debugger.core.messages.EduAIDebuggerCoreBundle
import org.jetbrains.annotations.NonNls

interface AIDebuggerServiceError {
  val messageKey: String

  fun message(): @NonNls String = EduAIDebuggerCoreBundle.message(messageKey)
}