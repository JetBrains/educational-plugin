package com.jetbrains.edu.learning.stepik.hyperskill.newProjectUI.notLoggedInPanel

import com.intellij.openapi.util.IconLoader
import com.intellij.ui.JBColor


fun getIconPath(path: String): String {
  val darkSuffix = "_dark"

  val isDark = !JBColor.isBright()

  val extension = path.substringAfterLast(".", missingDelimiterValue = "")
  val pathWithNoExt = path.removeSuffix(".$extension")

  val suffix = if (isDark) darkSuffix else ""
  return "$pathWithNoExt$suffix${if (extension.isNotEmpty()) ".$extension" else ""}"
}

fun loadIcon(
  basePath: String,
  classLoader: ClassLoader
) = IconLoader.getIcon(getIconPath(basePath), classLoader)
