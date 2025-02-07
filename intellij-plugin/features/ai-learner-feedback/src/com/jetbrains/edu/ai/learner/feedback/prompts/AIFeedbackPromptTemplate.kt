package com.jetbrains.edu.ai.learner.feedback.prompts

import com.jetbrains.educational.ml.core.prompt.PromptTemplate

enum class AIFeedbackPromptTemplate(override val fileName: String) : PromptTemplate {
  FEEDBACK_SYSTEM_PROMPT("system.ftl"),
  FEEDBACK_USER_PROMPT("user.ftl")
}