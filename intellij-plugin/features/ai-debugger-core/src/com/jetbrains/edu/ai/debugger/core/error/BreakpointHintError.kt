package com.jetbrains.edu.ai.debugger.core.error

import com.jetbrains.edu.ai.messages.BUNDLE
import org.jetbrains.annotations.PropertyKey

enum class BreakpointHintError(@PropertyKey(resourceBundle = BUNDLE) override val messageKey: String) : AIDebuggerServiceError {
  // TODO: add more types of errors
  NO_BREAKPOINT_HINTS("action.Educational.AiDebuggerNotification.no.suitable.breakpoint.hints.found"),
  DEFAULT_ERROR("action.Educational.AiDebuggerNotification.modal.session.fail");
}