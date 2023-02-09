package com.jetbrains.edu.learning.marketplace

import com.jetbrains.edu.learning.courseFormat.MarketplaceUserInfo
import com.jetbrains.edu.learning.marketplace.api.MarketplaceAccount
import com.jetbrains.edu.learning.marketplace.settings.MarketplaceSettings

fun loginFakeMarketplaceUser() {
  val account = MarketplaceAccount()
  account.userInfo = MarketplaceUserInfo("Test User")
  MarketplaceSettings.INSTANCE.account = account
}

fun logoutFakeMarketplaceUser() {
  MarketplaceSettings.INSTANCE.account = null
}