package com.jetbrains.edu.ai.error.explanation.prompts

import com.jetbrains.educational.ml.core.context.Context

data class ErrorExplanationContext(
  val programmingLanguage: String,
  val code: String,
  val stderr: String,
) : Context