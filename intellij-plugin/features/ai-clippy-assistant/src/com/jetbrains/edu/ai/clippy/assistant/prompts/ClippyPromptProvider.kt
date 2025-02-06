package com.jetbrains.edu.ai.clippy.assistant.prompts

import com.jetbrains.educational.ml.core.prompt.PromptProvider

const val PROMPT_DIRECTORY = "/prompts/clippy"

object ClippyPromptProvider : PromptProvider(PROMPT_DIRECTORY) {
    fun buildSystemPrompt(programmingLanguage: String): String {
        val context = SystemContext(programmingLanguage)
        return ClippyPromptTemplate.SYSTEM_PROMPT.process(context)
    }

    fun buildUserPrompt(taskDescription: String, code: String, initialCode: String): String {
        val context = UserContext(taskDescription, code, initialCode)
        return ClippyPromptTemplate.USER_PROMPT.process(context)
    }
}