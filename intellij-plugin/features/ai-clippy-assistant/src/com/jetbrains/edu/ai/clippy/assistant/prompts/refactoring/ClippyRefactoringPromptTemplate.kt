package com.jetbrains.edu.ai.clippy.assistant.prompts.refactoring

import com.jetbrains.educational.ml.core.prompt.PromptTemplate

enum class ClippyRefactoringPromptTemplate(override val fileName: String) : PromptTemplate {
    SYSTEM_PROMPT("refactoring/system.ftl"),
    USER_PROMPT("refactoring/user4.ftl")
}