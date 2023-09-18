package com.jetbrains.edu.learning.marketplace.settings

import com.intellij.execution.process.ProcessIOExecutorService
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.jetbrains.edu.learning.marketplace.MarketplaceNotificationUtils.showFailedToChangeSharingPreferenceNotification
import com.jetbrains.edu.learning.marketplace.api.MarketplaceAccount
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.marketplace.getJBAUserInfo
import com.jetbrains.edu.learning.onError
import java.util.concurrent.CompletableFuture

class MarketplaceSettings {

  private var account: MarketplaceAccount? = null

  var solutionsSharing: Boolean? = null

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

  fun updateSharingPreference(state: Boolean) {
    CompletableFuture.runAsync({
      MarketplaceSubmissionsConnector.getInstance().changeSharingPreference(state).onError {
        showFailedToChangeSharingPreferenceNotification()
        return@runAsync
      }
      solutionsSharing = state
    }, ProcessIOExecutorService.INSTANCE)
  }

  companion object {
    private val LOG = logger<MarketplaceSettings>()

    val INSTANCE: MarketplaceSettings
      get() = service()
  }
}