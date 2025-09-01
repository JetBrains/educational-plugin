package com.jetbrains.edu.learning

import com.intellij.ide.plugins.IdeaPluginDescriptorImpl
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

typealias PluginMainDescriptor = IdeaPluginDescriptorImpl

val PluginMainDescriptor.contentModules: List<IdeaPluginDescriptorImpl> get() = content.modules.map { it.requireDescriptor() }

internal fun updateAction(action: AnAction, e: AnActionEvent) {
  action.beforeActionPerformedUpdate(e)
}