package com.jetbrains.edu.ai.error.explanation

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension

interface ErrorAnalyzer {
  fun getStackTrace(stderr: String): List<Pair<String, Int>> = emptyList()

  fun isException(stderr: String): Boolean = false

  companion object {
    val EP_NAME = LanguageExtension<ErrorAnalyzer>("Educational.errorAnalyzer")

    fun getInstance(language: Language): ErrorAnalyzer? = EP_NAME.forLanguage(language)
  }
}