package com.jetbrains.edu.learning.command

import com.intellij.ide.plugins.HeadlessPluginsInstaller
import com.intellij.openapi.extensions.PluginId

// BACKCOMPAT: 2023.3. Inline
fun installPlugins(pluginIds: Set<PluginId>, courseName: String): CommandResult {
  val installedPlugins = HeadlessPluginsInstaller.installPlugins(pluginIds)
  if (installedPlugins.size != pluginIds.size) {
    return CommandResult.Error("Failed to install plugins for `$courseName` course")
  }
  return CommandResult.Ok
}