package com.jetbrains.edu.learning.marketplace.api

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.diagnostic.logger
import com.intellij.ui.JBAccountInfoService
import com.jetbrains.edu.learning.api.EduLoginConnector
import com.jetbrains.edu.learning.authUtils.OAuthUtils.GrantType.JBA_TOKEN_EXCHANGE
import com.jetbrains.edu.learning.authUtils.TokenInfo
import com.jetbrains.edu.learning.checkIsBackgroundThread
import com.jetbrains.edu.learning.courseFormat.MarketplaceUserInfo
import com.jetbrains.edu.learning.createRetrofitBuilder
import com.jetbrains.edu.learning.executeHandlingExceptions
import com.jetbrains.edu.learning.marketplace.HUB_AUTH_URL
import com.jetbrains.edu.learning.marketplace.JET_BRAINS_ACCOUNT
import com.jetbrains.edu.learning.marketplace.MarketplaceNotificationUtils.showLoginFailedNotification
import com.jetbrains.edu.learning.marketplace.MarketplaceNotificationUtils.showReloginToJBANeededNotification
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnectorUtils.EDU_CLIENT_ID
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnectorUtils.EDU_CLIENT_SECRET
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnectorUtils.MARKETPLACE_CLIENT_ID
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnectorUtils.checkIsGuestAndSave
import com.jetbrains.edu.learning.marketplace.settings.MarketplaceSettings
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

abstract class MarketplaceAuthConnector : EduLoginConnector<MarketplaceAccount, MarketplaceUserInfo>() {

  override val clientSecret: String
    get() = EDU_CLIENT_SECRET

  override val clientId: String
    get() = EDU_CLIENT_ID

  override fun doAuthorize(vararg postLoginActions: Runnable,
                           authorizationPlace: EduCounterUsageCollector.AuthorizationPlace) {
    this.authorizationPlace = authorizationPlace
    setPostLoginActions(postLoginActions.asList())
    login()
  }

  override fun isLoggedIn(): Boolean {
    if (JBAccountInfoService.getInstance()?.idToken != null && account != null) return true
    return autoLogIn()
  }

  fun autoLogIn(): Boolean {
    var success = false
    invokeAndWaitIfNeeded {
      ApplicationManager.getApplication().executeOnPooledThread {
        val jbAuthService = JBAccountInfoService.getInstance() ?: error("Failed to get JBAccountInfoService")
        val jbaAccessToken = getJBAccessToken(jbAuthService)
        if (jbaAccessToken == null) return@executeOnPooledThread

        val account = MarketplaceSettings.INSTANCE.hubAccount
        if (account != null) {
          success = true
          return@executeOnPooledThread
        }
        success = getHubTokenAndSave(jbaAccessToken)
      }
    }
    return success
  }

  private fun getJBAccessToken(jbAuthService: JBAccountInfoService): String? {
    checkIsBackgroundThread()

    val jbAccessToken: String? = try {
      jbAuthService.accessToken.get(30, TimeUnit.SECONDS)
    }
    catch (e: InterruptedException) {
      LOG.warn(e)
      null
    }
    catch (e: ExecutionException) {
      LOG.warn(e)
      null
    }
    return jbAccessToken
  }

  private fun login() {
    val jbAuthService = JBAccountInfoService.getInstance() ?: error("Failed to log in to $platformName")
    if (jbAuthService.userData == null) {
      jbAuthService.invokeJBALogin({ getHubTokenAndSave(jbAuthService) }, { showLoginFailedNotification(JET_BRAINS_ACCOUNT) })
      return
    }
    getHubTokenAndSave(jbAuthService)
  }

  private fun getHubTokenAndSave(jbAuthService: JBAccountInfoService) {
    ApplicationManager.getApplication().executeOnPooledThread {
      val jbAccessToken: String? = getJBAccessToken(jbAuthService)
      if (jbAccessToken == null) {
        LOG.warn("Log in failed: JetBrains account token is null")
        showReloginToJBANeededNotification()
        return@executeOnPooledThread
      }

      if (!getHubTokenAndSave(jbAccessToken)) error("Current hub user is anonymous")
    }
  }

  private fun getHubTokenAndSave(jbAccessToken: String): Boolean {
    checkIsBackgroundThread()

    val hubTokenInfo = retrieveHubToken(jbAccessToken)

    val account = MarketplaceAccount(hubTokenInfo.expiresIn)
    val currentUser = getUserInfo(account, hubTokenInfo.accessToken) ?: return false
    return checkIsGuestAndSave(currentUser, account, hubTokenInfo)
  }

  private fun retrieveHubToken(jbaAccessToken: String): TokenInfo {
    val response = extensionGrantsEndpoint.exchangeTokens(JBA_TOKEN_EXCHANGE, jbaAccessToken,
                                                          MARKETPLACE_CLIENT_ID).executeHandlingExceptions()
    return response?.body() ?: error("Failed to obtain hub token via extension grants. JetBrains account access token is null")
  }

  private val extensionGrantsEndpoint: HubExtensionGrantsEndpoints
    get() = getHubExtensionGrantsEndpoints()

  private fun getHubExtensionGrantsEndpoints(): HubExtensionGrantsEndpoints {
    val retrofit = createRetrofitBuilder(
      HUB_AUTH_URL, connectionPool,
      accessToken = Base64.getEncoder().encodeToString("$EDU_CLIENT_ID:$EDU_CLIENT_SECRET".toByteArray()),
      authHeaderValue = AUTH_TYPE_BASIC)
      .addConverterFactory(converterFactory)
      .build()

    return retrofit.create(HubExtensionGrantsEndpoints::class.java)
  }

  companion object {
    private val LOG = logger<MarketplaceAuthConnector>()

    private const val AUTH_TYPE_BASIC = "Basic"
  }
}