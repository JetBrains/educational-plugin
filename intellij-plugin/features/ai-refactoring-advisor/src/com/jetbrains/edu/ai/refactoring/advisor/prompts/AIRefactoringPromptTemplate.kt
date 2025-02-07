package com.jetbrains.edu.ai.refactoring.advisor.prompts

import com.jetbrains.educational.ml.core.prompt.PromptTemplate

enum class AIRefactoringPromptTemplate(override val fileName: String) : PromptTemplate {
  SYSTEM_PROMPT("system.ftl"),
  USER_PROMPT("user4.ftl")
}