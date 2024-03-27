package com.jetbrains.edu.learning.eduAssistant.core

import com.jetbrains.edu.learning.messages.EduCoreBundle

enum class AssistantError(val errorMessage: String) {
  AuthError("${EduCoreBundle.message("action.Educational.NextStepHint.error.common")} ${EduCoreBundle.message("action.Educational.NextStepHint.error.auth")}"),
  NetworkError("${EduCoreBundle.message("action.Educational.NextStepHint.error.common")} ${EduCoreBundle.message("action.Educational.NextStepHint.error.network")}"),
  TooManyRequests("${EduCoreBundle.message("action.Educational.NextStepHint.error.common")} ${EduCoreBundle.message("action.Educational.NextStepHint.error.requests")}"),
  NoCompiledCode(EduCoreBundle.message("action.Educational.NextStepHint.error.no.compiled.code")),
  EmptyDiff("${EduCoreBundle.message("action.Educational.NextStepHint.error.common")} ${EduCoreBundle.message("action.Educational.NextStepHint.error.unknown")}"),
  UnknownError("${EduCoreBundle.message("action.Educational.NextStepHint.error.common")} ${EduCoreBundle.message("action.Educational.NextStepHint.error.unknown")}")
}
