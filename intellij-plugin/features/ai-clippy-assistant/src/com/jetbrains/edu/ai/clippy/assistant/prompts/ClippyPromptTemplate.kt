package com.jetbrains.edu.ai.clippy.assistant.prompts

import com.jetbrains.educational.ml.core.prompt.PromptTemplate

enum class ClippyPromptTemplate(override val fileName: String) : PromptTemplate {
    SYSTEM_PROMPT("system.ftl"),
    USER_PROMPT("user4.ftl")
}