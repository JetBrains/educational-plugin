package com.jetbrains.edu.learning.marketplace.api

import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.diagnostic.logger
import com.intellij.ui.JBAccountInfoService
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.jetbrains.edu.coursecreator.CCNotificationUtils
import com.jetbrains.edu.learning.api.EduLoginConnector
import com.jetbrains.edu.learning.authUtils.OAuthUtils.GrantType.JBA_TOKEN_EXCHANGE
import com.jetbrains.edu.learning.authUtils.TokenInfo
import com.jetbrains.edu.learning.authUtils.requestFocus
import com.jetbrains.edu.learning.courseFormat.JBAccountUserInfo
import com.jetbrains.edu.learning.createRetrofitBuilder
import com.jetbrains.edu.learning.executeHandlingExceptions
import com.jetbrains.edu.learning.marketplace.HUB_AUTH_URL
import com.jetbrains.edu.learning.marketplace.JET_BRAINS_ACCOUNT
import com.jetbrains.edu.learning.marketplace.MARKETPLACE
import com.jetbrains.edu.learning.marketplace.MarketplaceNotificationUtils.showInstallMarketplacePluginNotification
import com.jetbrains.edu.learning.marketplace.MarketplaceNotificationUtils.showLoginFailedNotification
import com.jetbrains.edu.learning.marketplace.MarketplaceNotificationUtils.showReloginToJBANeededNotification
import com.jetbrains.edu.learning.marketplace.MarketplaceOAuthBundle
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import java.util.*

abstract class MarketplaceAuthConnector : EduLoginConnector<MarketplaceAccount, JBAccountUserInfo>() {
  override val clientSecret: String = MarketplaceOAuthBundle.value("eduHubClientSecret")

  override val clientId: String = MarketplaceOAuthBundle.value("eduHubClientId")

  override fun doAuthorize(vararg postLoginActions: Runnable,
                           authorizationPlace: EduCounterUsageCollector.AuthorizationPlace) {
    this.authorizationPlace = authorizationPlace
    ApplicationManager.getApplication().executeOnPooledThread {
      login(*postLoginActions)
    }
  }

  fun setPostLoginActions(vararg postLoginActions: Runnable): List<Runnable> {
    val requestFocus = Runnable { runInEdt { requestFocus() } }
    val showNotification = Runnable {
      val userName = account?.userInfo?.getFullName() ?: return@Runnable
      CCNotificationUtils.showLoginSuccessfulNotification(userName)
    }
    return postLoginActions.asList() + listOf(requestFocus, showNotification)
  }

  @RequiresBackgroundThread
  override fun isLoggedIn(): Boolean {
    if (account == null) return false

    val isJbaTokenAvailable = isJBAccessTokenAvailable()
    if (!isJbaTokenAvailable) {
      LOG.info("JetBrains account access token not available for logged-in user ${account?.userInfo?.name}")
    }

    return isJbaTokenAvailable
  }

  private fun login(vararg postLoginActions: Runnable) {
    val jbAccountInfoService = getJBAccountInfoServiceWithNotification() ?: return

    val currentAccount = account
    if (jbAccountInfoService.userData == null) {
      invokeJBALogin(jbAccountInfoService, setPostLoginActions(*postLoginActions))
      return
    }
    else if (currentAccount == null || !currentAccount.isJBAccessTokenAvailable(jbAccountInfoService)) {
      LOG.info("JB access token not available. Relogin needed to proceed")
      showReloginToJBANeededNotification(invokeJBALoginAction(jbAccountInfoService, *postLoginActions))
    }
  }

  private fun getJBAccountInfoServiceWithNotification(): JBAccountInfoService? {
    val jbAuthService = JBAccountInfoService.getInstance()
    return if (jbAuthService == null) {
      LOG.warn("JBAccountInfoService is null")
      showInstallMarketplacePluginNotification(
        EduCoreBundle.message("error.failed.login.to.subsystem", MARKETPLACE),
        NotificationType.ERROR
      )
      null
    }
    else {
      jbAuthService
    }
  }

  fun invokeJBALogin(jbAuthService: JBAccountInfoService, postLoginActions: List<Runnable>) {
    jbAuthService.invokeJBALogin({postLoginActions.forEach { it.run() }}, { showLoginFailedNotification(JET_BRAINS_ACCOUNT) })
  }

  @RequiresBackgroundThread
  private fun isJBAccessTokenAvailable(): Boolean {
    val jbaInfoService = getJBAccountInfoServiceWithNotification() ?: return false
    return account?.isJBAccessTokenAvailable(jbaInfoService) ?: return false
  }

  /*
  Possible return values:
    - null means no jba token in IDE and you need to relogin (possible when user logged in from toolbox or when token refresh inside IDE failed)
    - exception means some network error
    - non-null value - correct hab token
   */
  @RequiresBackgroundThread
  fun loadHubToken(currentAccount: MarketplaceAccount): String? {
    val jbAccountInfoService: JBAccountInfoService = getJBAccountInfoServiceWithNotification() ?: return null
    val jbAccessToken: String = currentAccount.getJBAccessToken(jbAccountInfoService) ?: return null

    return retrieveHubToken(jbAccessToken).accessToken
  }

  fun invokeJBALoginAction(jbAuthService: JBAccountInfoService, vararg postLoginActions: Runnable) = object : AnAction(EduCoreBundle.message("action.relogin.to.jba")) {
    override fun actionPerformed(e: AnActionEvent) {
      LOG.warn("Invoking login to JB account")
      invokeJBALogin(jbAuthService, setPostLoginActions(*postLoginActions))
    }
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
      accessToken = Base64.getEncoder().encodeToString("$clientId:$clientSecret".toByteArray()),
      authHeaderValue = AUTH_TYPE_BASIC)
      .addConverterFactory(converterFactory)
      .build()

    return retrofit.create(HubExtensionGrantsEndpoints::class.java)
  }

  override fun getFreshAccessToken(userAccount: MarketplaceAccount?, accessToken: String?): String? {
    return accessToken
  }

  companion object {
    private val LOG = logger<MarketplaceAuthConnector>()
    private val MARKETPLACE_CLIENT_ID: String = MarketplaceOAuthBundle.value("marketplaceHubClientId")

    private const val AUTH_TYPE_BASIC = "Basic"
  }
}