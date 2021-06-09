package com.jetbrains.edu.learning

import com.intellij.ide.plugins.DisabledPluginsState
import com.intellij.openapi.extensions.PluginId
import com.jetbrains.edu.learning.messages.EduCoreBundle

fun enablePlugins(ids: List<PluginId>) {
  DisabledPluginsState.enablePluginsById(ids, true)
  restartIDE(EduCoreBundle.message("required.plugin.were.enabled"))
}