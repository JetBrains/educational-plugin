package com.jetbrains.edu.python.learning

import com.jetbrains.edu.learning.EduLanguageDecorator
import icons.PythonIcons
import javax.swing.Icon

class PyLanguageDecorator : EduLanguageDecorator {
  override fun getLanguageScriptUrl(): String = javaClass.classLoader.getResource("/code-mirror/python.js").toExternalForm()
  override fun getDefaultHighlightingMode(): String = "python"
  override fun getLogo(): Icon = PythonIcons.Python.Python_logo
}
