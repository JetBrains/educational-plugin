package com.jetbrains.edu.learning.marketplace

import com.intellij.ide.plugins.marketplace.MarketplaceRequests
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.updateSettings.impl.pluginsAdvertisement.PluginsAdvertiser


@Suppress("UnstableApiUsage")
fun getBuildNumberForRequests(): String = MarketplaceRequests.getInstance().getBuildForPluginRepositoryRequests()

fun installAndEnablePlugin(pluginIds: Set<PluginId>, onSuccess: Runnable) = PluginsAdvertiser.installAndEnable(pluginIds, onSuccess)