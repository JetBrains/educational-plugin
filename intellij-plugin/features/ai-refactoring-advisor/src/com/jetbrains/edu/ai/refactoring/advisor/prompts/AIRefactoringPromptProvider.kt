package com.jetbrains.edu.ai.refactoring.advisor.prompts

import com.jetbrains.educational.ml.core.prompt.PromptProvider

object AIRefactoringPromptProvider : PromptProvider() {
  fun buildSystemPrompt(refactoringContext: AIRefactoringContext): String =
    AIRefactoringPromptTemplate.SYSTEM_PROMPT.process(refactoringContext)

  fun buildUserPrompt(refactoringContext: AIRefactoringContext): String =
    AIRefactoringPromptTemplate.USER_PROMPT.process(refactoringContext)
}