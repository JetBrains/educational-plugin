package com.jetbrains.edu.learning.taskToolWindow.ui

import com.intellij.ui.JBColor

fun getIconPath(path: String): String {
  val extension = path.substringAfterLast(".", missingDelimiterValue = "")
  val pathWithNoExt = path.removeSuffix(".$extension")
  val suffix = if (!JBColor.isBright()) "_dark" else ""
  return "$pathWithNoExt$suffix${if (extension.isNotEmpty()) ".$extension" else ""}"
}
