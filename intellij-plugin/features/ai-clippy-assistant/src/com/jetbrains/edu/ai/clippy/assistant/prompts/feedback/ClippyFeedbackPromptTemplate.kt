package com.jetbrains.edu.ai.clippy.assistant.prompts.feedback

import com.jetbrains.educational.ml.core.prompt.PromptTemplate

enum class ClippyFeedbackPromptTemplate(override val fileName: String) : PromptTemplate {
  FEEDBACK_SYSTEM_PROMPT("feedback/system.ftl"),
  FEEDBACK_USER_PROMPT("feedback/user.ftl")
}