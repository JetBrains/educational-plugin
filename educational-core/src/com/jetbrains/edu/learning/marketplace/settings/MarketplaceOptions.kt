package com.jetbrains.edu.learning.marketplace.settings

import com.jetbrains.edu.learning.api.EduLoginConnector
import com.jetbrains.edu.learning.marketplace.MARKETPLACE
import com.jetbrains.edu.learning.marketplace.MARKETPLACE_PROFILE_PATH
import com.jetbrains.edu.learning.marketplace.api.MarketplaceAccount
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.settings.OAuthLoginOptions

class MarketplaceOptions : OAuthLoginOptions<MarketplaceAccount>() {
  override val connector: EduLoginConnector<MarketplaceAccount, *>
    get() = MarketplaceConnector.getInstance()

  override fun getDisplayName(): String = MARKETPLACE

  override fun profileUrl(account: MarketplaceAccount): String = MARKETPLACE_PROFILE_PATH

  override fun prepareLogOutLink() {}
}