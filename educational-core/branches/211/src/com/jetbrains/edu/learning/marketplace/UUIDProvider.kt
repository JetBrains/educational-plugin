package com.jetbrains.edu.learning.marketplace

import com.intellij.internal.statistic.DeviceIdManager

object UUIDProvider {
  @Suppress("UnstableApiUsage")
  fun getUUID(): String {
    // BACKCOMPAT 221: replace with com.intellij.openapi.updateSettings.impl.PluginDownloader.getMarketplaceDownloadsUUID()
    return DeviceIdManager.getOrGenerateId(object : DeviceIdManager.DeviceIdToken {}, "MarketplaceDownloads")
  }
}