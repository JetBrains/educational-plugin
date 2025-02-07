package com.jetbrains.edu.ai.error.explanation.prompts

import com.jetbrains.educational.ml.core.prompt.PromptProvider

private const val TEMPLATES_FOLDER: String = "/templates/errorExplanation"

object ErrorExplanationPromptProvider : PromptProvider(TEMPLATES_FOLDER) {
  fun getSystemTemplate(): String = ErrorExplanationPromptTemplate.SYSTEM_PROMPT.process()

  fun getUserTemplate(context: ErrorExplanationContext): String = ErrorExplanationPromptTemplate.USER_PROMPT.process(context)
}