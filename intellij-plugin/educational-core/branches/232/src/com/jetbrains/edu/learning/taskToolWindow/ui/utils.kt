package com.jetbrains.edu.learning.taskToolWindow.ui

import com.intellij.openapi.actionSystem.ActionToolbar

// BACKCOMPAT: 2023.3. Inline it.
fun ActionToolbar.setupLayoutStrategy() {
  layoutPolicy = ActionToolbar.NOWRAP_LAYOUT_POLICY
}
