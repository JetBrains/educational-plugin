package com.jetbrains.edu.learning.ui

import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.laf.UIThemeLookAndFeelInfoImpl

// BACKCOMPAT: 2023.2. Inline it
fun getCurrentThemeName(): String? {
  @Suppress("UnstableApiUsage")
  val lookAndFeel = LafManager.getInstance().currentUIThemeLookAndFeel as? UIThemeLookAndFeelInfoImpl
  return lookAndFeel?.name
}

// BACKCOMPAT: 2023.2. Inline it
fun getCurrentThemeId(): String? {
  @Suppress("UnstableApiUsage")
  val lookAndFeel = LafManager.getInstance().currentUIThemeLookAndFeel as? UIThemeLookAndFeelInfoImpl
  return lookAndFeel?.theme?.id
}

// BACKCOMPAT: 2023.2. Inline it
fun isUnderWin10LookAndFeel(): Boolean {
  return false
}