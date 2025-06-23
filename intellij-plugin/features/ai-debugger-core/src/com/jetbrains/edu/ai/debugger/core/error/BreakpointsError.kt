package com.jetbrains.edu.ai.debugger.core.error

import com.jetbrains.edu.ai.messages.BUNDLE
import org.jetbrains.annotations.PropertyKey

enum class BreakpointsError(@PropertyKey(resourceBundle = BUNDLE) override val messageKey: String) : AIDebuggerServiceError {
  // TODO: add more types of errors
  NO_BREAKPOINTS("action.Educational.AiDebuggerNotification.no.suitable.breakpoints.found"),
  DEFAULT_ERROR("action.Educational.AiDebuggerNotification.modal.session.fail");
}
