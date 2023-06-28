package com.jetbrains.edu.learning.command

import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task

@Suppress("UNUSED_PARAMETER")
open class InstallAndEnableTaskHeadlessImpl(pluginIds: Set<PluginId>, onSuccess: Runnable) : Task.Modal(null, "", true) {
  override fun run(indicator: ProgressIndicator) {
    error("Not supported on 2022.3 platform")
  }
}
