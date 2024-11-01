package com.jetbrains.edu.learning.ai

import com.jetbrains.educational.core.enum.TranslationLanguage
import com.jetbrains.educational.translation.format.domain.TranslationVersion

data class TranslationProperties(val language: TranslationLanguage, val version: TranslationVersion)
