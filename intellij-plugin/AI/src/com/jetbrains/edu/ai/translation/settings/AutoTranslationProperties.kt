package com.jetbrains.edu.ai.translation.settings

import com.jetbrains.educational.core.format.enum.TranslationLanguage

data class AutoTranslationProperties(val language: TranslationLanguage, val autoTranslate: Boolean)