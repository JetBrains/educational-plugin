package com.jetbrains.edu.learning

import com.intellij.ide.plugins.IdeaPluginDescriptorImpl
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

internal fun collectFromModules(
  pluginDescriptor: IdeaPluginDescriptorImpl,
  collect: (moduleDescriptor: IdeaPluginDescriptorImpl) -> Unit
) {
  for (module in pluginDescriptor.content.modules) {
    collect(module.requireDescriptor())
  }
}

internal fun updateAction(action: AnAction, e: AnActionEvent) {
  action.beforeActionPerformedUpdate(e)
}