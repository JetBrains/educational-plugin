package com.jetbrains.edu.learning.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.jetbrains.edu.learning.authUtils.OAuthAccount
import com.jetbrains.edu.learning.authUtils.OAuthUtils.GrantType.AUTHORIZATION_CODE
import com.jetbrains.edu.learning.authUtils.OAuthUtils.GrantType.REFRESH_TOKEN
import com.jetbrains.edu.learning.authUtils.TokenInfo
import com.jetbrains.edu.learning.createRetrofitBuilder
import com.jetbrains.edu.learning.executeHandlingExceptions
import com.jetbrains.edu.learning.isUnitTestMode
import okhttp3.ConnectionPool
import retrofit2.converter.jackson.JacksonConverterFactory

abstract class EduOAuthConnector<Account : OAuthAccount<*>> {
  protected abstract val account: Account?

  protected abstract val baseUrl: String

  protected open val baseOAuthTokenUrl: String = "oauth2/token/"

  protected abstract val clientId: String

  protected abstract val clientSecret: String

  abstract val objectMapper: ObjectMapper

  protected val connectionPool: ConnectionPool = ConnectionPool()

  protected val converterFactory: JacksonConverterFactory by lazy {
    JacksonConverterFactory.create(objectMapper)
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
}