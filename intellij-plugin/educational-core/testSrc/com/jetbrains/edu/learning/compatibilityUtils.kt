package com.jetbrains.edu.learning

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ex.ActionUtil

// BACKCOMPAT: 2025.1. Inline it
typealias PluginMainDescriptor = com.intellij.ide.plugins.PluginMainDescriptor

// BACKCOMPAT: 2025.1. Inline it.
internal fun updateAction(action: AnAction, e: AnActionEvent) {
  ActionUtil.updateAction(action, e)
}