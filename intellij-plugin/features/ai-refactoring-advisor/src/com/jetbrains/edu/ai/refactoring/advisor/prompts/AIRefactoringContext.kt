package com.jetbrains.edu.ai.refactoring.advisor.prompts

import com.jetbrains.educational.ml.core.context.Context

data class AIRefactoringUserContext(
  val code: String,
  val initialCode: String,
  val taskDescription: String,
): Context

data class AIRefactoringSystemContext(
  val programmingLanguage: String
): Context