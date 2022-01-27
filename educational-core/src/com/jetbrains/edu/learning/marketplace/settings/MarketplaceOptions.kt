package com.jetbrains.edu.learning.marketplace.settings

import com.jetbrains.edu.learning.marketplace.MARKETPLACE
import com.jetbrains.edu.learning.marketplace.MARKETPLACE_PROFILE_PATH
import com.jetbrains.edu.learning.marketplace.api.MarketplaceAccount
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.settings.LoginOptions
import javax.swing.event.HyperlinkEvent

class MarketplaceOptions : LoginOptions<MarketplaceAccount>() {
  override fun getCurrentAccount(): MarketplaceAccount? = MarketplaceSettings.INSTANCE.account

  override fun setCurrentAccount(account: MarketplaceAccount?) {
    MarketplaceSettings.INSTANCE.account = account
    val connector = MarketplaceConnector.getInstance()
    if (account != null) {
      connector.notifyUserLoggedIn()
    }
    else {
      connector.notifyUserLoggedOut()
    }
  }

  override fun profileUrl(account: MarketplaceAccount): String = MARKETPLACE_PROFILE_PATH

  override fun createAuthorizeListener(): LoginListener {
    return object : LoginListener() {
      override fun authorize(e: HyperlinkEvent?) {
        MarketplaceConnector.getInstance().doAuthorize(Runnable {
          lastSavedAccount = getCurrentAccount()
          updateLoginLabels()
        })
      }
    }
  }

  override fun getDisplayName(): String = MARKETPLACE
}