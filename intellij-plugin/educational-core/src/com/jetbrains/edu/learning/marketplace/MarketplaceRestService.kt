package com.jetbrains.edu.learning.marketplace

import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector

class MarketplaceRestService : BaseMarketplaceRestService(MARKETPLACE) {
  override val courseIdParamName: String = "course_id"
  override fun getServiceName(): String = MarketplaceConnector.getInstance().serviceName
}