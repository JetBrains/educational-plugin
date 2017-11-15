package com.jetbrains.edu.java

import com.jetbrains.edu.learning.EduLanguageDecorator
import icons.EducationalCoreIcons
import javax.swing.Icon

class JLanguageDecorator : EduLanguageDecorator {
  override fun getLanguageScriptUrl(): String = javaClass.getResource("/code_mirror/clike.js").toExternalForm()
  override fun getDefaultHighlightingMode(): String = "text/x-java"
  override fun getLogo(): Icon = EducationalCoreIcons.JavaLogo
}
