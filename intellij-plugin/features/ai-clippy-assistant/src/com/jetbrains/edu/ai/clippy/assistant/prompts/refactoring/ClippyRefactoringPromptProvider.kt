package com.jetbrains.edu.ai.clippy.assistant.prompts.refactoring

import com.jetbrains.educational.ml.core.prompt.PromptProvider

object ClippyRefactoringPromptProvider : PromptProvider() {
    fun buildSystemPrompt(programmingLanguage: String): String {
        val context = SystemContext(programmingLanguage)
        return ClippyRefactoringPromptTemplate.SYSTEM_PROMPT.process(context)
    }

    fun buildUserPrompt(taskDescription: String, code: String, initialCode: String): String {
        val context = UserContext(taskDescription, code, initialCode)
        return ClippyRefactoringPromptTemplate.USER_PROMPT.process(context)
    }
}