package com.jetbrains.edu.java

import com.jetbrains.edu.learning.EduLanguageDecorator
import icons.EducationalCoreIcons
import javax.swing.Icon

class JLanguageDecorator : EduLanguageDecorator {
  override fun getLanguageScriptUrl(): String = javaClass.classLoader.getResource("/code-mirror/clike.js").toExternalForm()
  override fun getDefaultHighlightingMode(): String = "text/x-java"
  override fun getLogo(): Icon = EducationalCoreIcons.JavaLogo
}
