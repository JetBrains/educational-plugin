package com.jetbrains.edu.learning.command

import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.updateSettings.impl.pluginsAdvertisement.InstallAndEnableTaskHeadlessImpl

// BACKCOMPAT: 2023.3. Inline
fun installPlugins(pluginIds: Set<PluginId>, courseName: String): CommandResult {
  var result: CommandResult = CommandResult.Ok
  @Suppress("UnstableApiUsage")
  ProgressManager.getInstance().run(object : InstallAndEnableTaskHeadlessImpl(pluginIds, {}) {
    override fun onThrowable(error: Throwable) {
      result = CommandResult.Error("Failed to install plugins for `$courseName` course", error)
    }
  })
  return result
}