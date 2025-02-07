package com.jetbrains.edu.ai.error.explanation.prompts

import com.jetbrains.educational.ml.core.prompt.PromptTemplate

enum class ErrorExplanationPromptTemplate(override val fileName: String) : PromptTemplate {
  SYSTEM_PROMPT("system.ftl"),
  USER_PROMPT("user.ftl")
}