package com.jetbrains.edu.learning.marketplace

import com.jetbrains.edu.learning.courseFormat.MarketplaceUserInfo
import com.jetbrains.edu.learning.marketplace.api.MarketplaceAccount
import com.jetbrains.edu.learning.marketplace.settings.MarketplaceSettings

fun loginFakeMarketplaceUser() {
  val account = MarketplaceAccount()
  account.userInfo = MarketplaceUserInfo("Test User")
  account.saveJwtToken("not empty jwt token")
  MarketplaceSettings.INSTANCE.account = account
}

fun logoutFakeMarketplaceUser() {
  MarketplaceSettings.INSTANCE.account = null
}