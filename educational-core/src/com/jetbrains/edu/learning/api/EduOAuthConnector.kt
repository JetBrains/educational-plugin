package com.jetbrains.edu.learning.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.Urls
import com.intellij.util.xmlb.annotations.Transient
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.authUtils.CustomAuthorizationServer
import com.jetbrains.edu.learning.authUtils.OAuthAccount
import com.jetbrains.edu.learning.authUtils.OAuthRestService.CODE_ARGUMENT
import com.jetbrains.edu.learning.authUtils.OAuthUtils
import com.jetbrains.edu.learning.authUtils.OAuthUtils.GrantType.AUTHORIZATION_CODE
import com.jetbrains.edu.learning.authUtils.OAuthUtils.GrantType.REFRESH_TOKEN
import com.jetbrains.edu.learning.authUtils.TokenInfo
import okhttp3.ConnectionPool
import org.jetbrains.annotations.NonNls
import org.jetbrains.ide.BuiltInServerManager
import org.jetbrains.ide.RestService
import retrofit2.converter.jackson.JacksonConverterFactory
import java.io.IOException
import java.util.regex.Pattern

abstract class EduOAuthConnector<Account : OAuthAccount<*>, SpecificUserInfo : UserInfo> {
  @get:Transient
  @set:Transient
  protected abstract var account: Account?

  protected abstract val authorizationUrl: String

  protected abstract val baseUrl: String

  protected open val baseOAuthTokenUrl: String = "oauth2/token/"

  protected abstract val clientId: String

  protected abstract val clientSecret: String

  abstract val objectMapper: ObjectMapper

  protected abstract val platformName: String

  protected val connectionPool: ConnectionPool = ConnectionPool()

  protected val converterFactory: JacksonConverterFactory by lazy {
    JacksonConverterFactory.create(objectMapper)
  }

  @get: Synchronized
  @set: Synchronized
  private var postLoginActions: Array<out Runnable>? = null

  @get: Synchronized
  @set: Synchronized
  private var submissionTabListener: EduLogInListener? = null

  open val serviceName: String by lazy {
    "${EduNames.EDU_PREFIX}/${platformName.toLowerCase()}"
  }

  protected open val oAuthServicePath: String by lazy {
    "/${RestService.PREFIX}/$serviceName/$OAUTH_SUFFIX"
  }

  abstract fun getUserInfo(account: Account, accessToken: String?): SpecificUserInfo?

  open fun doAuthorize(vararg postLoginActions: Runnable) {
    if (!OAuthUtils.checkBuiltinPortValid()) return

    setPostLoginActions(*postLoginActions)
    BrowserUtil.browse(authorizationUrl)
  }

  abstract fun login(code: String): Boolean

  /**
   * @param postLoginActions - actions to be happened after successful authorization
   * this could be actions to update some UI e.x. to hide panels with login offering messages
   * on every doAuthorize call these actions are being rewritten
   */
  protected open fun setPostLoginActions(vararg postLoginActions: Runnable) {
    this.postLoginActions = postLoginActions
  }

  /**
   * @param logInListener - listener to update submissions list and
   * [com.jetbrains.edu.learning.submissions.SubmissionsTab]
   */
  fun setSubmissionTabListener(logInListener: EduLogInListener) {
    submissionTabListener = logInListener
  }

  fun getCurrentUserInfo(): SpecificUserInfo? {
    val currentAccount = account ?: return null
    return getUserInfo(currentAccount, currentAccount.getAccessToken())
  }

  private fun getEduOAuthEndpoints(): EduOAuthEndpoints =
    createRetrofitBuilder(baseUrl.withTrailingSlash(), connectionPool)
      .addConverterFactory(converterFactory)
      .build()
      .create(EduOAuthEndpoints::class.java)

  /**
   * No need to pass any arguments by default, but you need to pass
   * account and accessToken for [com.jetbrains.edu.learning.api.EduOAuthConnector.getUserInfo]
   */
  protected inline fun <reified Endpoints> getEndpoints(
    account: Account? = this.account,
    accessToken: String? = account?.getAccessToken(),
    baseUrl: String = this.baseUrl
  ): Endpoints {
    if (!isUnitTestMode && account != null && !account.isUpToDate()) {
      refreshTokens()
    }

    return createRetrofitBuilder(baseUrl.withTrailingSlash(), connectionPool, accessToken)
      .addConverterFactory(converterFactory)
      .build()
      .create(Endpoints::class.java)
  }

  protected open fun getNewTokens(): TokenInfo {
    val currentAccount = account ?: error("No logged in user")
    val refreshToken = currentAccount.getRefreshToken() ?: error("Refresh token is null")
    val response = getEduOAuthEndpoints()
      .refreshTokens(baseOAuthTokenUrl, REFRESH_TOKEN, clientId, clientSecret, refreshToken)
      .executeHandlingExceptions()
    return response?.body() ?: error("Failed to refresh tokens")
  }

  @Throws(IOException::class)
  private fun createCustomServer(): CustomAuthorizationServer {
    return CustomAuthorizationServer.create(platformName, oAuthServicePath) { code: String, _: String ->
      if (!login(code)) "Failed to login to $platformName" else null
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
      Urls.newHttpUrl("localhost:${currentPort}", oAuthServicePath).toString()
    }

  @JvmOverloads
  fun getOAuthPattern(suffix: String = """\?$CODE_ARGUMENT=(\w+)"""): Pattern {
    return "^.*$oAuthServicePath$suffix".toPattern()
  }

  fun getServicePattern(suffix: String): Pattern = "^.*$serviceName$suffix".toPattern()

  protected open fun refreshTokens() {
    val currentAccount = account ?: return
    val tokens = getNewTokens()
    currentAccount.tokenExpiresIn = tokens.expiresIn
    currentAccount.saveTokens(tokens)
  }

  protected fun retrieveLoginToken(code: String, redirectUri: String): TokenInfo? {
    val response = getEduOAuthEndpoints()
      .getTokens(baseOAuthTokenUrl, clientId, clientSecret, redirectUri, code, AUTHORIZATION_CODE)
      .executeHandlingExceptions()
    return response?.body()
  }

  fun isLoggedIn(): Boolean = account != null

  /**
   * Designed to be called in a single place `*Settings.setAccount` for every platform
   * @see com.jetbrains.edu.learning.EduSettings.setUser
   */
  fun notifyUserLoggedIn() {
    postLoginActions?.forEach {
      it.run()
    }
    submissionTabListener?.userLoggedIn()
  }

  /**
   * Designed to be called in a single place `*Settings.setAccount` for every platform
   * @see com.jetbrains.edu.learning.EduSettings.setUser
   */
  fun notifyUserLoggedOut() = submissionTabListener?.userLoggedOut()

  companion object {
    @JvmStatic
    protected val LOG = Logger.getInstance(EduOAuthConnector::class.java)

    @JvmStatic
    @NonNls
    protected val OAUTH_SUFFIX: String = "oauth"

    /**
     * Retrofit builder needs url to be with trailing slash
     */
    protected fun String.withTrailingSlash(): String = if (!endsWith('/')) "$this/" else this
  }
}