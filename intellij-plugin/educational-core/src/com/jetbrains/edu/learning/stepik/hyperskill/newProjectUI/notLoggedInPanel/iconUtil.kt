package com.jetbrains.edu.learning.stepik.hyperskill.newProjectUI.notLoggedInPanel

import com.intellij.openapi.util.IconLoader
import com.intellij.ui.ColorUtil
import com.intellij.util.ui.JBUI

fun getIconPath(path: String): String {
  val darkSuffix = "_dark"

  // copy-pasted from `com.intellij.platform.ide.newUiOnboarding.NewUiOnboardingUtil`
  val isDark = ColorUtil.isDark(JBUI.CurrentTheme.GotItTooltip.background(false))

  val extension = path.substringAfterLast(".", missingDelimiterValue = "")
  val pathWithNoExt = path.removeSuffix(".$extension")

  val suffix = if (isDark) darkSuffix else ""
  return "$pathWithNoExt$suffix${if (extension.isNotEmpty()) ".$extension" else ""}"
}

fun loadIcon(
  basePath: String,
  classLoader: ClassLoader
) = IconLoader.getIcon(getIconPath(basePath), classLoader)