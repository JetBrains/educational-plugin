package com.jetbrains.edu.scala

import com.jetbrains.edu.learning.EduLanguageDecorator
import icons.EducationalCoreIcons
import javax.swing.Icon

class ScalaLanguageDecorator : EduLanguageDecorator {
  override fun getLanguageScriptUrl(): String = javaClass.classLoader.getResource("/code-mirror/clike.js").toExternalForm()
  override fun getDefaultHighlightingMode(): String = "text/x-scala"
  override fun getLogo(): Icon = EducationalCoreIcons.ScalaLogo
}
