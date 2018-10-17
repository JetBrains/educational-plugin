package com.jetbrains.edu.kotlin

import com.jetbrains.edu.learning.EduLanguageDecorator

class KtLanguageDecorator : EduLanguageDecorator {
  override fun getLanguageScriptUrl(): String = javaClass.classLoader.getResource("/code-mirror/clike.js").toExternalForm()
  override fun getDefaultHighlightingMode(): String = "text/x-java"
}
