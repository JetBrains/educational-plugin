package com.jetbrains.edu.ai.clippy.assistant.prompts

import com.jetbrains.educational.ml.core.prompt.PromptTemplate

enum class ClippyFeedbackPromptTemplate(override val fileName: String) : PromptTemplate {
  FEEDBACK_SYSTEM_PROMPT("system.ftl"),
  FEEDBACK_USER_PROMPT("user.ftl")
}