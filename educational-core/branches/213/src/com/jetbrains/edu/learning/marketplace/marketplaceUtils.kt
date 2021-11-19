package com.jetbrains.edu.learning.marketplace

import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.updateSettings.impl.pluginsAdvertisement.installAndEnable

@Suppress("UnstableApiUsage")

fun installAndEnablePlugin(pluginIds: Set<PluginId>, onSuccess: Runnable) = installAndEnable(pluginIds, onSuccess)