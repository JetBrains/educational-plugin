package com.jetbrains.edu.learning.ui

import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.laf.UIThemeBasedLookAndFeelInfo
import com.intellij.openapi.actionSystem.impl.ActionButton

fun getCurrentThemeName(): String? {
  val lookAndFeel = LafManager.getInstance().currentLookAndFeel as? UIThemeBasedLookAndFeelInfo
  return lookAndFeel?.name
}

// BACKCOMPAT: 2023.2. Inline it
fun getCurrentThemeId(): String? {
  val lookAndFeel = LafManager.getInstance().currentLookAndFeel as? UIThemeBasedLookAndFeelInfo
  return lookAndFeel?.theme?.id
}

// BACKCOMPAT: 2023.1. Remove it
@Suppress("UnusedReceiverParameter")
fun ActionButton.setLook() {
}