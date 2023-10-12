package com.jetbrains.edu.learning.ui

import com.intellij.ide.ui.LafManager
import com.intellij.openapi.actionSystem.impl.ActionButton

// BACKCOMPAT: 2023.2. Inline it
fun getCurrentThemeName(): String? {
  @Suppress("UnstableApiUsage")
  return LafManager.getInstance().currentUIThemeLookAndFeel?.name
}

// BACKCOMPAT: 2023.2. Inline it
fun getCurrentThemeId(): String? {
  @Suppress("UnstableApiUsage")
  return LafManager.getInstance().currentUIThemeLookAndFeel?.id
}

// BACKCOMPAT: 2023.1. Remove it
fun ActionButton.setLook() {
}