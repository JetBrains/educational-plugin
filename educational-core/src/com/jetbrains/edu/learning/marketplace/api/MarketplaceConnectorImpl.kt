package com.jetbrains.edu.learning.marketplace.api

import com.jetbrains.edu.learning.marketplace.HUB_AUTH_URL

class MarketplaceConnectorImpl : MarketplaceConnector() {
  override val baseUrl: String = HUB_AUTH_URL
}