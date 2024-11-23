package com.jetbrains.edu.learning.marketplace.settings

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.ui.JBAccountInfoService
import com.jetbrains.edu.learning.marketplace.api.MarketplaceAccount
import com.jetbrains.edu.learning.marketplace.getJBAUserInfo

@Service(Service.Level.APP)
class MarketplaceSettings {

  private var account: MarketplaceAccount? = null

  fun getMarketplaceAccount(): MarketplaceAccount? {
    if (!isJBALoggedIn()) {
      account = null
      return null
    }
    val currentAccount = account
    val jbaUserInfo = getJBAUserInfo()
    if (jbaUserInfo == null) {
      val accountName = account?.userInfo?.name
      LOG.error("User info is null${if (accountName != null) " for $accountName account" else ""}")
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

    fun isJBALoggedIn(): Boolean = JBAccountInfoService.getInstance()?.userData != null

    val INSTANCE: MarketplaceSettings
      get() = service()
  }
}