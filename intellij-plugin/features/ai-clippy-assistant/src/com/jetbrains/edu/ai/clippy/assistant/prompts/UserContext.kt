package com.jetbrains.edu.ai.clippy.assistant.prompts

import com.jetbrains.educational.ml.core.context.Context

data class UserContext(
    val taskDescription: String,
    val code: String,
    val initialCode: String
) : Context
