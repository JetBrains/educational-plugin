package com.jetbrains.edu.learning.marketplace.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.jetbrains.edu.learning.marketplace.HUB_PROFILE_PATH
import com.jetbrains.edu.learning.marketplace.MARKETPLACE
import com.jetbrains.edu.learning.marketplace.api.MarketplaceAccount
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.api.MarketplaceUserInfo
import com.jetbrains.edu.learning.settings.OauthOptions
import javax.swing.event.HyperlinkEvent

class MarketplaceOptions : OauthOptions<MarketplaceAccount>() {
  override fun getCurrentAccount(): MarketplaceAccount? = MarketplaceSettings.INSTANCE.account

  override fun setCurrentAccount(account: MarketplaceAccount?) {
    MarketplaceSettings.INSTANCE.account = account
    val messageBus = ApplicationManager.getApplication().messageBus
    if (account != null) {
      messageBus.syncPublisher(MarketplaceConnector.AUTHORIZATION_TOPIC).userLoggedIn()
    }
    else {
      messageBus.syncPublisher(MarketplaceConnector.AUTHORIZATION_TOPIC).userLoggedOut()
    }
  }

  override fun getProfileUrl(userInfo: Any): String {
    return if (userInfo is MarketplaceUserInfo) {
      "$HUB_PROFILE_PATH${userInfo.id}"
    }
    else {
      logger<MarketplaceOptions>().error("userInfo is not instance of MarketplaceUserInfo")
      HUB_PROFILE_PATH
    }
  }

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