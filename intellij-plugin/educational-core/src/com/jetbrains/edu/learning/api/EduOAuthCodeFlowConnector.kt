package com.jetbrains.edu.learning.api

import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.Urls
import com.intellij.util.io.origin
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.EduLogInListener
import com.jetbrains.edu.learning.authUtils.*
import com.jetbrains.edu.learning.courseFormat.UserInfo
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.network.executeCall
import com.jetbrains.edu.learning.network.executeHandlingExceptions
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.edu.learning.onError
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.QueryStringDecoder
import org.apache.commons.codec.binary.Base64
import org.apache.http.client.utils.URIBuilder
import org.jetbrains.ide.BuiltInServerManager
import org.jetbrains.ide.RestService
import java.net.HttpURLConnection.*
import java.net.URI
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.text.Charsets.US_ASCII

/**
 * Base class for OAuthConnectors using [Authorization Code Flow with Proof Key for Code Exchange (PKCE)](https://auth0.com/docs/get-started/authentication-and-authorization-flow/authorization-code-flow-with-proof-key-for-code-exchange-pkce)
 */
abstract class EduOAuthCodeFlowConnector<Account : OAuthAccount<*>, SpecificUserInfo : UserInfo> : EduLoginConnector<Account, SpecificUserInfo>() {
  protected open val redirectHost = "localhost"

  protected open val baseOAuthTokenUrl: String = "oauth2/token/"

  private var state: String = generateSafeRandomString()

  private var codeVerifier: String = generateSafeRandomString()
  private var codeChallenge: String? = null

  protected abstract val authorizationUrlBuilder: URIBuilder

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
    authorizationPlace: AuthorizationPlace
  ) {
    // Unit tests use completely different port and
    // `OAuthUtils.checkBuiltinPortValid()` always returns false here since it relies only on the default production port number
    if (!isUnitTestMode && !OAuthUtils.checkBuiltinPortValid()) return

    this.authorizationPlace = authorizationPlace
    setPostLoginActions(postLoginActions.asList())
    val url = generateAuthorizationUrl()
    EduBrowser.getInstance().browse(url.toString())
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
   * @see com.jetbrains.edu.learning.EduSettings.user
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
   * @see com.jetbrains.edu.learning.EduSettings.user
   */
  @Synchronized
  fun notifyUserLoggedOut() {
    submissionTabListener?.userLoggedOut()

    val place = authorizationPlace ?: AuthorizationPlace.UNKNOWN
    EduCounterUsageCollector.logOutSucceed(platformName, place)
    authorizationPlace = null
  }

  /**
   * Must be synchronized to avoid race condition
   * `receivedState` MUST BE COMPARED with the `state` before any action performed
   */
  abstract fun login(code: String): Boolean

  @Synchronized
  fun doLogout(authorizationPlace: AuthorizationPlace = AuthorizationPlace.UNKNOWN) {
    this.authorizationPlace = authorizationPlace
    account = null
  }

  protected fun getRedirectUri(): String {
    val currentPort = BuiltInServerManager.getInstance().port
    return Urls.newHttpUrl("$redirectHost:${currentPort}", oAuthServicePath).toString()
  }

  private fun getNewTokens(): TokenInfo {
    val currentAccount = account ?: error("No logged-in user")
    val refreshToken = currentAccount.getRefreshToken() ?: error("Refresh token is null")
    val response = getEduOAuthEndpoints()
      .refreshTokens(baseOAuthTokenUrl, OAuthUtils.GrantType.REFRESH_TOKEN, clientId, refreshToken)
      .executeCall().onError { error(EduCoreBundle.message("error.failed.to.refresh.tokens")) }
    if (response.isSuccessful) return response.body() ?: error("Failed to refresh token")

    // logout the user if the server did not accept Hyperskill refresh token
    if (response.code() in BROKEN_TOKEN_RESPONSE_CODES && response.errorBody()?.string() == "{\"error\": \"invalid_grant\"}") {
      doLogout()
      EduNotificationManager.showErrorNotification(
        title = @Suppress("DialogTitleCapitalization") EduCoreBundle.message("error.authorization.error"),
        content = EduCoreBundle.message("notification.hyperskill.no.next.activity.login.content")
      )
    }
    error("Failed to refresh token")
  }

  protected fun retrieveLoginToken(code: String, redirectUri: String, codeVerifierFieldName: String = "code_verifier"): TokenInfo? {
    val codeVerifierField = mapOf(codeVerifierFieldName to codeVerifier)
    val response = getEduOAuthEndpoints()
      .getTokens(baseOAuthTokenUrl, clientId, redirectUri, code, OAuthUtils.GrantType.AUTHORIZATION_CODE, codeVerifierField)
      .executeHandlingExceptions()
    return response?.body()
  }

  open fun refreshTokens() {
    //synchronized block needed here because a number of threads can try to refresh tokens at the same time (e.g. 15 for Stepik startup
    // which causes tokens corruption
    synchronized(this) {
      val currentAccount = account ?: return
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

  private fun generateAuthorizationUrl(): URI {
    state = generateSafeRandomString()
    codeVerifier = generateSafeRandomString()

    val bytes = codeVerifier.toByteArray(US_ASCII)
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(bytes)
    codeChallenge = Base64.encodeBase64URLSafeString(digest)

    return authorizationUrlBuilder
      .addParameter(STATE, state)
      .addParameter("code_challenge", codeChallenge)
      .addParameter("code_challenge_method", "S256")
      .build()
  }

  private fun generateSafeRandomString(): String {
    val sr = SecureRandom()
    val bytes = ByteArray(32)
    sr.nextBytes(bytes)
    return Base64.encodeBase64URLSafeString(bytes)
  }

  fun isValidOAuthRequest(request: FullHttpRequest, urlDecoder: QueryStringDecoder): Boolean {
    val origin = request.origin
    if (origin.isNullOrEmpty()) return RestService.getStringParameter(STATE, urlDecoder) == state
    LOG.warn("RestService got request with empty origin")
    return false
  }

  protected inline fun <reified Endpoints> getEndpoints(): Endpoints {
    return getEndpoints(accessToken = account?.getAccessToken())
  }

  companion object {
    private val LOG = Logger.getInstance(EduOAuthCodeFlowConnector::class.java)
    private val BROKEN_TOKEN_RESPONSE_CODES = arrayOf(HTTP_BAD_REQUEST, HTTP_FORBIDDEN, HTTP_UNAUTHORIZED)
  }
}