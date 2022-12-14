package com.jetbrains.edu.learning.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.authUtils.OAuthAccount
import com.jetbrains.edu.learning.authUtils.OAuthRestService.CODE_ARGUMENT
import com.jetbrains.edu.learning.authUtils.OAuthUtils.GrantType.AUTHORIZATION_CODE
import com.jetbrains.edu.learning.authUtils.OAuthUtils.GrantType.REFRESH_TOKEN
import com.jetbrains.edu.learning.authUtils.TokenInfo
import com.jetbrains.edu.learning.courseFormat.UserInfo
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector.AuthorizationPlace
import okhttp3.ConnectionPool
import okhttp3.Interceptor
import org.jetbrains.annotations.NonNls
import org.jetbrains.ide.RestService
import retrofit2.converter.jackson.JacksonConverterFactory
import java.util.regex.Pattern

/**
 * Base class for all connectors managing login.
 * Connectors, using [Authorization Code Flow](https://auth0.com/docs/get-started/authentication-and-authorization-flow/authorization-code-flow)
 * should be inherited from [EduOAuthCodeFlowConnector]
 */
abstract class EduLoginConnector<Account : OAuthAccount<*>, SpecificUserInfo : UserInfo> {
  open var account: Account? = null

  protected open val redirectHost = "localhost"

  protected abstract val baseUrl: String

  protected open val baseOAuthTokenUrl: String = "oauth2/token/"

  protected abstract val clientId: String

  protected abstract val clientSecret: String

  abstract val objectMapper: ObjectMapper

  /**
   * Name of the platform used for developing purposes
   */
  protected abstract val platformName: String
    @NonNls get

  protected val connectionPool: ConnectionPool = ConnectionPool()

  protected val converterFactory: JacksonConverterFactory by lazy {
    JacksonConverterFactory.create(objectMapper)
  }

  abstract fun doAuthorize(
    vararg postLoginActions: Runnable,
    authorizationPlace: AuthorizationPlace = AuthorizationPlace.UNKNOWN
  )

  /**
   * Must be changed only with synchronization
   */
  protected var authorizationPlace: AuthorizationPlace? = null

  /**
   * Must be changed only with synchronization
   */
  private var postLoginActions: List<Runnable>? = null

  /**
   * Must be changed only with synchronization
   */
  private var submissionTabListener: EduLogInListener? = null

  protected open val requestInterceptor: Interceptor? = null

  open val serviceName: String by lazy {
    "${EduNames.EDU_PREFIX}/${platformName.lowercase()}"
  }

  protected open val oAuthServicePath: String by lazy {
    "/${RestService.PREFIX}/$serviceName/$OAUTH_SUFFIX"
  }

  abstract fun getUserInfo(account: Account, accessToken: String?): SpecificUserInfo?

  @Synchronized
  fun doLogout(authorizationPlace: AuthorizationPlace = AuthorizationPlace.UNKNOWN) {
    this.authorizationPlace = authorizationPlace
    account = null
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
   * account and accessToken for [com.jetbrains.edu.learning.api.EduLoginConnector.getUserInfo]
   * because access token is not saved at the moment we want to get userInfo and check if current user isGuest
   */
  protected inline fun <reified Endpoints> getEndpoints(
    account: Account? = this.account,
    accessToken: String? = account?.getAccessToken(),
    baseUrl: String = this.baseUrl
  ): Endpoints {
    val freshAccessToken = if (!isUnitTestMode && account != null && !account.isUpToDate()) {
      refreshTokens()
      account.getAccessToken()
    }
    else {
      accessToken
    }

    return createRetrofitBuilder(baseUrl.withTrailingSlash(), connectionPool, freshAccessToken, customInterceptor = requestInterceptor)
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
    return response?.body() ?: error(EduCoreBundle.message("error.failed.to.refresh.tokens"))
  }

  @JvmOverloads
  fun getOAuthPattern(suffix: String = """\?$CODE_ARGUMENT=(\w+)"""): Pattern {
    return "^.*$oAuthServicePath$suffix".toPattern()
  }

  fun getServicePattern(suffix: String): Pattern = "^.*$serviceName$suffix".toPattern()

  protected open fun refreshTokens() {
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

  protected fun retrieveLoginToken(code: String, redirectUri: String): TokenInfo? {
    val response = getEduOAuthEndpoints()
      .getTokens(baseOAuthTokenUrl, clientId, clientSecret, redirectUri, code, AUTHORIZATION_CODE)
      .executeHandlingExceptions()
    return response?.body()
  }

  open fun isLoggedIn(): Boolean = account != null

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

    val place = authorizationPlace ?: AuthorizationPlace.UNKNOWN
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

    val place = authorizationPlace ?: AuthorizationPlace.UNKNOWN
    EduCounterUsageCollector.logOutSucceed(platformName, place)
    authorizationPlace = null
  }

  companion object {
    @JvmStatic
    protected val LOG = Logger.getInstance(EduLoginConnector::class.java)

    @JvmStatic
    @NonNls
    protected val OAUTH_SUFFIX: String = "oauth"

    /**
     * Retrofit builder needs url to be with trailing slash
     */
    protected fun String.withTrailingSlash(): String = if (!endsWith('/')) "$this/" else this
  }
}