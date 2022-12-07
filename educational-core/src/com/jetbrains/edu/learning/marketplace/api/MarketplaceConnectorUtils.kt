package com.jetbrains.edu.learning.marketplace.api

import com.intellij.openapi.diagnostic.logger
import com.jetbrains.edu.learning.authUtils.TokenInfo
import com.jetbrains.edu.learning.courseFormat.MarketplaceUserInfo
import com.jetbrains.edu.learning.marketplace.MarketplaceOAuthBundle

object MarketplaceConnectorUtils {
  val EDU_CLIENT_ID: String = MarketplaceOAuthBundle.value("eduHubClientId")
  val EDU_CLIENT_SECRET: String = MarketplaceOAuthBundle.value("eduHubClientSecret")
  val MARKETPLACE_CLIENT_ID: String = MarketplaceOAuthBundle.value("marketplaceHubClientId")
  private val LOG = logger<MarketplaceAuthConnector>()

  fun MarketplaceAuthConnector.checkIsGuestAndSave(currentUser: MarketplaceUserInfo, account: MarketplaceAccount, hubTokenInfo: TokenInfo): Boolean {
    if (currentUser.isGuest) {
      // it means that session is broken, so we should force user to re-login
      LOG.warn("User ${currentUser.name} is anonymous")
      this.account = null
      return false
    }
    account.userInfo = currentUser

    this.account = account
    account.saveTokens(hubTokenInfo)
    return true
  }
}