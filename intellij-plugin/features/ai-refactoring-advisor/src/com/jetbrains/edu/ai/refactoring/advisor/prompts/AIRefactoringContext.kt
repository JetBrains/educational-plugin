package com.jetbrains.edu.ai.refactoring.advisor.prompts

import com.jetbrains.educational.ml.core.context.Context

data class AIRefactoringContext(
  val code: String,
  val programmingLanguage: String,
  val initialCode: String,
  val taskDescription: String,
): Context