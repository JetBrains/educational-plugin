package com.jetbrains.edu.learning.api

import com.intellij.ide.BrowserUtil
import com.intellij.util.Urls
import com.jetbrains.edu.learning.EduLogInListener
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.authUtils.CustomAuthorizationServer
import com.jetbrains.edu.learning.authUtils.OAuthAccount
import com.jetbrains.edu.learning.authUtils.OAuthUtils
import com.jetbrains.edu.learning.authUtils.TokenInfo
import com.jetbrains.edu.learning.courseFormat.UserInfo
import com.jetbrains.edu.learning.executeHandlingExceptions
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import org.jetbrains.ide.BuiltInServerManager
import java.io.IOException

/**
 * Base class for OAuthConnectors using [Authorization Code Flow](https://auth0.com/docs/get-started/authentication-and-authorization-flow/authorization-code-flow)
 */
abstract class EduOAuthCodeFlowConnector<Account : OAuthAccount<*>, SpecificUserInfo : UserInfo>: EduLoginConnector<Account, SpecificUserInfo>() {
  protected open val redirectHost = "localhost"

  protected open val baseOAuthTokenUrl: String = "oauth2/token/"

  protected abstract val authorizationUrl: String

  /**
   * Must be changed only with synchronization
   */
  private var postLoginActions: List<Runnable>? = null

  /**
   * Must be changed only with synchronization
   */
  private var submissionTabListener: EduLogInListener? = null

  @Synchronized
  override fun doAuthorize(
    vararg postLoginActions: Runnable,
    authorizationPlace: EduCounterUsageCollector.AuthorizationPlace
  ) {
    if (!OAuthUtils.checkBuiltinPortValid()) return

    this.authorizationPlace = authorizationPlace
    setPostLoginActions(postLoginActions.asList())
    BrowserUtil.browse(authorizationUrl)
  }

  /**
   * @param postLoginActions - actions to be happened after successful authorization
   * this could be actions to update some UI e.x. to hide panels with login offering messages
   * on every doAuthorize call these actions are being rewritten
   */
  @Synchronized
  protected open fun setPostLoginActions(postLoginActions: List<Runnable>) {
    this.postLoginActions = postLoginActions
  }

  /**
   * @param logInListener - listener to update submissions list and
   * [com.jetbrains.edu.learning.submissions.SubmissionsTab]
   */
  @Synchronized
  fun setSubmissionTabListener(logInListener: EduLogInListener) {
    submissionTabListener = logInListener
  }

  /**
   * Designed to be called in a single place `*Settings.setAccount` for every platform
   * @see com.jetbrains.edu.learning.EduSettings.setUser
   */
  @Synchronized
  fun notifyUserLoggedIn() {
    postLoginActions?.forEach {
      it.run()
    }
    submissionTabListener?.userLoggedIn()

    val place = authorizationPlace ?: EduCounterUsageCollector.AuthorizationPlace.UNKNOWN
    EduCounterUsageCollector.logInSucceed(platformName, place)
    authorizationPlace = null
  }

  /**
   * Designed to be called in a single place `*Settings.setAccount` for every platform
   * @see com.jetbrains.edu.learning.EduSettings.setUser
   */
  @Synchronized
  fun notifyUserLoggedOut() {
    submissionTabListener?.userLoggedOut()

    val place = authorizationPlace ?: EduCounterUsageCollector.AuthorizationPlace.UNKNOWN
    EduCounterUsageCollector.logOutSucceed(platformName, place)
    authorizationPlace = null
  }

  /**
   * Must be synchronized to avoid race condition
   */
  abstract fun login(code: String): Boolean

  @Synchronized
  fun doLogout(authorizationPlace: EduCounterUsageCollector.AuthorizationPlace = EduCounterUsageCollector.AuthorizationPlace.UNKNOWN) {
    this.authorizationPlace = authorizationPlace
    account = null
  }

  @Throws(IOException::class)
  private fun createCustomServer(): CustomAuthorizationServer {
    return CustomAuthorizationServer.create(platformName, oAuthServicePath) { code: String, _: String ->
      if (!login(code)) "Failed to log in to $platformName" else null
    }
  }

  protected open fun getRedirectUri(): String =
    if (EduUtils.isAndroidStudio()) {
      val runningServer = CustomAuthorizationServer.getServerIfStarted(platformName)
      val server = runningServer ?: createCustomServer()
      server.handlingUri
    }
    else {
      // port is already checked to be valid
      val currentPort = BuiltInServerManager.getInstance().port
      Urls.newHttpUrl("$redirectHost:${currentPort}", oAuthServicePath).toString()
    }

  private fun getNewTokens(): TokenInfo {
    val currentAccount = account ?: error("No logged-in user")
    val refreshToken = currentAccount.getRefreshToken() ?: error("Refresh token is null")
    val response = getEduOAuthEndpoints()
      .refreshTokens(baseOAuthTokenUrl, OAuthUtils.GrantType.REFRESH_TOKEN, clientId, clientSecret, refreshToken)
      .executeHandlingExceptions()
    return response?.body() ?: error(EduCoreBundle.message("error.failed.to.refresh.tokens"))
  }

  protected fun retrieveLoginToken(code: String, redirectUri: String): TokenInfo? {
    val response = getEduOAuthEndpoints()
      .getTokens(baseOAuthTokenUrl, clientId, clientSecret, redirectUri, code, OAuthUtils.GrantType.AUTHORIZATION_CODE)
      .executeHandlingExceptions()
    return response?.body()
  }

  open fun refreshTokens() {
    //synchronized block needed here because a number of threads can try to refresh tokens at the same time (e.g. 15 for Stepik startup
    // which causes tokens corruption
    synchronized(this) {
      val currentAccount = account ?: return
      if (currentAccount.isUpToDate()) {
        return
      }
      val tokens = getNewTokens()
      currentAccount.tokenExpiresIn = tokens.expiresIn
      currentAccount.saveTokens(tokens)
      LOG.info("successfully refreshed tokens for $platformName")
    }
  }

  override fun getCurrentUserInfo(): SpecificUserInfo? {
    val currentAccount = account ?: return null
    return getUserInfo(currentAccount, currentAccount.getAccessToken())
  }

  abstract fun getUserInfo(account: Account, accessToken: String?): SpecificUserInfo?

  override fun getFreshAccessToken(userAccount: Account?, accessToken: String?): String? {
    return if (!isUnitTestMode && userAccount != null && !userAccount.isUpToDate()) {
      refreshTokens()
      account?.getAccessToken()
    }
    else {
      accessToken
    }
  }

  protected inline fun <reified Endpoints> getEndpoints(): Endpoints {
    return getEndpoints(accessToken = account?.getAccessToken())
  }
}