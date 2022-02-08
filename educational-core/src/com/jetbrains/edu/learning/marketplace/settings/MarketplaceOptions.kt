package com.jetbrains.edu.learning.marketplace.settings

import com.jetbrains.edu.learning.api.EduOAuthConnector
import com.jetbrains.edu.learning.marketplace.MARKETPLACE_PROFILE_PATH
import com.jetbrains.edu.learning.marketplace.api.MarketplaceAccount
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.settings.OAuthLoginOptions

class MarketplaceOptions : OAuthLoginOptions<MarketplaceAccount>() {
  override val connector: EduOAuthConnector<MarketplaceAccount, *>
    get() = MarketplaceConnector.getInstance()

  override fun profileUrl(account: MarketplaceAccount): String = MARKETPLACE_PROFILE_PATH
}