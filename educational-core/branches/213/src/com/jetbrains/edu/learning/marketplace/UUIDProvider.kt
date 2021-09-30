package com.jetbrains.edu.learning.marketplace

import com.intellij.openapi.updateSettings.impl.PluginDownloader

object UUIDProvider {
  fun getUUID(): String = PluginDownloader.getMarketplaceDownloadsUUID()
}