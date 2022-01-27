package com.jetbrains.edu.learning.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.messages.MessageBus
import com.intellij.util.messages.Topic
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.authUtils.OAuthAccount
import com.jetbrains.edu.learning.authUtils.OAuthUtils.GrantType.AUTHORIZATION_CODE
import com.jetbrains.edu.learning.authUtils.OAuthUtils.GrantType.REFRESH_TOKEN
import com.jetbrains.edu.learning.authUtils.TokenInfo
import okhttp3.ConnectionPool
import retrofit2.converter.jackson.JacksonConverterFactory

abstract class EduOAuthConnector<Account : OAuthAccount<*>, SpecificUserInfo : UserInfo> : Disposable {
  protected abstract val account: Account?

  private val applicationMessageBus: MessageBus = ApplicationManager.getApplication().messageBus

  private var authorizationMessageBus = applicationMessageBus.connect()

  protected abstract val authorizationTopicName: String

  private val authorizationTopic by lazy {
    Topic.create(authorizationTopicName, EduLogInListener::class.java)
  }

  protected abstract val baseUrl: String

  protected open val baseOAuthTokenUrl: String = "oauth2/token/"

  protected abstract val clientId: String

  protected abstract val clientSecret: String

  abstract val objectMapper: ObjectMapper

  protected val connectionPool: ConnectionPool = ConnectionPool()

  protected val converterFactory: JacksonConverterFactory by lazy {
    JacksonConverterFactory.create(objectMapper)
  }

  abstract fun getUserInfo(account: Account, accessToken: String?): SpecificUserInfo?

  override fun dispose() = authorizationMessageBus.disconnect()

  fun getCurrentUserInfo(): SpecificUserInfo? {
    val currentAccount = account ?: return null
    return getUserInfo(currentAccount, currentAccount.getAccessToken())
  }

  private fun getEduOAuthEndpoints(): EduOAuthEndpoints {
    return createRetrofitBuilder(baseUrl, connectionPool)
      .addConverterFactory(converterFactory)
      .build()
      .create(EduOAuthEndpoints::class.java)
  }

  protected inline fun <reified Endpoints> getEndpoints(
    account: Account? = null,
    accessToken: String? = null,
    baseUrl: String = this.baseUrl
  ): Endpoints {
    if (!isUnitTestMode && account != null && !account.isUpToDate()) {
      refreshTokens()
    }

    return createRetrofitBuilder(baseUrl, connectionPool, accessToken)
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

  protected fun refreshTokens() {
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

  fun notifyUserLoggedIn() = applicationMessageBus.syncPublisher(authorizationTopic).userLoggedIn()

  fun notifyUserLoggedOut() = applicationMessageBus.syncPublisher(authorizationTopic).userLoggedOut()

  fun subscribe(eduLogInListener: EduLogInListener) {
    authorizationMessageBus.subscribe(authorizationTopic, eduLogInListener)
  }

  /**
   * Drops existing log in listeners and add new one
   */
  fun reconnectAndSubscribe(eduLogInListener: EduLogInListener) {
    authorizationMessageBus.disconnect()
    authorizationMessageBus = applicationMessageBus.connect()
    subscribe(eduLogInListener)
  }
}