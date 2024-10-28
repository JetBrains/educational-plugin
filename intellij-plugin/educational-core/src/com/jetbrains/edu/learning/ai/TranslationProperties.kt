package com.jetbrains.edu.learning.ai

import com.jetbrains.educational.core.enum.Language
import com.jetbrains.educational.translation.format.domain.TranslationVersion

data class TranslationProperties(val language: Language, val version: TranslationVersion)
