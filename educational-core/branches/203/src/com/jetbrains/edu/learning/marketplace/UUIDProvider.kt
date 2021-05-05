package com.jetbrains.edu.learning.marketplace

import com.intellij.internal.statistic.DeviceIdManager

object UUIDProvider {
  @Suppress("UnstableApiUsage")
  fun getUUID(): String {
    return DeviceIdManager.getOrGenerateId("MarketplaceDownloads")
  }
}