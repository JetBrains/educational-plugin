package com.jetbrains.edu.kotlin

import com.jetbrains.edu.learning.EduLanguageDecorator
import org.jetbrains.kotlin.idea.KotlinIcons
import javax.swing.Icon

class KtLanguageDecorator : EduLanguageDecorator {
  override fun getLanguageScriptUrl(): String = javaClass.classLoader.getResource("/code-mirror/clike.js").toExternalForm()
  override fun getDefaultHighlightingMode(): String = "text/x-java"
  override fun getLogo(): Icon = KotlinIcons.SMALL_LOGO
}
