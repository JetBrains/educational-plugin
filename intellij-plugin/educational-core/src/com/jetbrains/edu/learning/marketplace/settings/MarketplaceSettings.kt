package com.jetbrains.edu.learning.marketplace.settings

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.ui.JBAccountInfoService
import com.jetbrains.edu.learning.authUtils.ConnectorUtils
import com.jetbrains.edu.learning.courseFormat.JBAccountUserInfo
import com.jetbrains.edu.learning.marketplace.api.MarketplaceAccount
import org.jetbrains.annotations.VisibleForTesting
import java.util.Base64

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

  @VisibleForTesting
  fun getJBAUserInfo(): JBAccountUserInfo? {
    val jbaIdToken = getJBAIdToken() ?: return null

    val parts: List<String> = jbaIdToken.split(DELIMITER)
    if (parts.size < 2) {
      error("JB Account id token data part is malformed")
    }
    val payload = String(Base64.getUrlDecoder().decode(parts[1]))
    return ConnectorUtils.createMapper().readValue(payload, JBAccountUserInfo::class.java)
  }

  private fun getJBAIdToken(): String? = JBAccountInfoService.getInstance()?.idToken

  companion object {
    private val LOG = logger<MarketplaceSettings>()

    private const val DELIMITER = "."

    fun isJBALoggedIn(): Boolean = JBAccountInfoService.getInstance()?.userData != null

    val INSTANCE: MarketplaceSettings
      get() = service()
  }
}