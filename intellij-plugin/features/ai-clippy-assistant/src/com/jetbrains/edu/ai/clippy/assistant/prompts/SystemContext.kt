package com.jetbrains.edu.ai.clippy.assistant.prompts

import com.jetbrains.educational.ml.core.context.Context

data class SystemContext(val programmingLanguage: String) : Context