package com.jetbrains.edu.learning.marketplace.settings

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.jetbrains.edu.learning.marketplace.api.MarketplaceAccount
import com.jetbrains.edu.learning.marketplace.getJBAUserInfo

class MarketplaceSettings {
  private var account: MarketplaceAccount? = null

  fun getMarketplaceAccount(): MarketplaceAccount? {
    if (!MarketplaceAccount.isJBALoggedIn()) {
      account = null
      return null
    }
    val currentAccount = account
    val jbaUserInfo = getJBAUserInfo()
    if (jbaUserInfo == null) {
      LOG.error("User info is null for account ${account?.userInfo?.name}")
      account = null
    }
    else if (currentAccount == null || !currentAccount.checkTheSameUserAndUpdate(jbaUserInfo)) {
      account = MarketplaceAccount(jbaUserInfo)
    }

    return account
  }

  fun setAccount(value: MarketplaceAccount?) {
    account = value
  }

  companion object {
    private val LOG = logger<MarketplaceSettings>()

    val INSTANCE: MarketplaceSettings
      get() = service()
  }
}