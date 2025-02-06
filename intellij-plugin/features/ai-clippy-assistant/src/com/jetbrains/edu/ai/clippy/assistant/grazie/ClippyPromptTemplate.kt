package com.jetbrains.edu.ai.clippy.assistant.grazie

import com.jetbrains.educational.ml.core.prompt.PromptTemplate

enum class ClippyPromptTemplate(override val fileName: String) : PromptTemplate {
  FEEDBACK_SYSTEM_PROMPT("feedback/system.ftl"),
  FEEDBACK_USER_PROMPT("feedback/user.ftl")
}