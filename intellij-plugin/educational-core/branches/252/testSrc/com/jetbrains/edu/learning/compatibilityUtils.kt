package com.jetbrains.edu.learning

import com.intellij.ide.plugins.IdeaPluginDescriptorImpl
import com.intellij.ide.plugins.contentModules
import com.intellij.ide.plugins.getMainDescriptor
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ex.ActionUtil

internal fun collectFromModules(
  pluginDescriptor: IdeaPluginDescriptorImpl,
  collect: (moduleDescriptor: IdeaPluginDescriptorImpl) -> Unit
) {
  for (module in pluginDescriptor.contentModules) {
    collect(module.getMainDescriptor())
  }
}

// BACKCOMPAT: 2025.1. Inline it.
internal fun updateAction(action: AnAction, e: AnActionEvent) {
  ActionUtil.updateAction(action, e)
}