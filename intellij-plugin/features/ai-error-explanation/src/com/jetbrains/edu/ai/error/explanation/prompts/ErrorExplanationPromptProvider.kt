package com.jetbrains.edu.ai.error.explanation.prompts

import com.jetbrains.educational.ml.core.prompt.PromptProvider

object ErrorExplanationPromptProvider : PromptProvider() {
  fun getSystemTemplate(): String = ErrorExplanationPromptTemplate.SYSTEM_PROMPT.process()

  fun getUserTemplate(context: ErrorExplanationContext): String = ErrorExplanationPromptTemplate.USER_PROMPT.process(context)
}