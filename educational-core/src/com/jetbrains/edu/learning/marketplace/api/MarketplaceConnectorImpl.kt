package com.jetbrains.edu.learning.marketplace.api

import com.jetbrains.edu.learning.marketplace.HUB_AUTH_URL
import com.jetbrains.edu.learning.marketplace.PLUGINS_REPOSITORY_URL

class MarketplaceConnectorImpl : MarketplaceConnector() {
  override val authUrl: String = HUB_AUTH_URL
  override val repositoryUrl: String = PLUGINS_REPOSITORY_URL
}