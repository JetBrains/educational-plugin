package com.jetbrains.edu.learning

import com.intellij.ide.plugins.PluginEnabler
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.jetbrains.edu.learning.messages.EduCoreBundle

fun enablePlugins(pluginsId: List<PluginId>) {
  val descriptors = pluginsId.mapNotNull { pluginId -> PluginManagerCore.getPlugin(pluginId) }
  @Suppress("UnstableApiUsage")
  PluginEnabler.HEADLESS.enable(descriptors)
  restartIDE(EduCoreBundle.message("required.plugin.were.enabled"))
}