package com.jetbrains.edu.ai.clippy.assistant.prompts.refactoring

import com.jetbrains.educational.ml.core.context.Context

data class SystemContext(val programmingLanguage: String) : Context