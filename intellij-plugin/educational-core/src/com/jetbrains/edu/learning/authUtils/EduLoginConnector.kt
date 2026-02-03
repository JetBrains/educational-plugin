package com.jetbrains.edu.learning.authUtils

import com.fasterxml.jackson.databind.ObjectMapper
import com.jetbrains.edu.learning.courseFormat.EduFormatNames
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.CODE_ARGUMENT
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.REST_PREFIX
import com.jetbrains.edu.learning.courseFormat.UserInfo
import com.jetbrains.edu.learning.network.createRetrofitBuilder
import okhttp3.ConnectionPool
import okhttp3.Interceptor
import org.jetbrains.annotations.NonNls
import retrofit2.converter.jackson.JacksonConverterFactory
import java.util.regex.Pattern

/**
 * Base class for all connectors managing login.
 * Connectors, using [Authorization Code Flow](https://auth0.com/docs/get-started/authentication-and-authorization-flow/authorization-code-flow)
 * should be inherited from [com.jetbrains.edu.learning.api.EduOAuthCodeFlowConnector]
 */
abstract class EduLoginConnector<UserAccount : Account<*>, SpecificUserInfo : UserInfo> {
  open var account: UserAccount? = null

  protected abstract val baseUrl: String

  protected abstract val clientId: String

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

  protected open val requestInterceptor: Interceptor? = null

  open val serviceName: String by lazy {
    "${EduFormatNames.EDU_PREFIX}/${platformName.lowercase()}"
  }

  open val oAuthServicePath: String by lazy {
    "/$REST_PREFIX/$serviceName/$OAUTH_SUFFIX"
  }

  abstract fun getCurrentUserInfo(): SpecificUserInfo?

  protected fun getEduOAuthEndpoints(): EduOAuthEndpoints =
    createRetrofitBuilder(baseUrl.withTrailingSlash(), connectionPool)
      .addConverterFactory(converterFactory)
      .build()
      .create(EduOAuthEndpoints::class.java)

  /**
   * No need to pass any arguments by default, but you need to pass
   * account and accessToken for [com.jetbrains.edu.learning.api.EduOAuthCodeFlowConnector.getUserInfo]
   * because access token is not saved at the moment we want to get userInfo and check if current user isGuest
   */
  protected inline fun <reified Endpoints> getEndpoints(
    account: UserAccount? = this.account,
    accessToken: String?,
    baseUrl: String = this.baseUrl
  ): Endpoints {

    val freshAccessToken: String? = getFreshAccessToken(account, accessToken)

    return createRetrofitBuilder(baseUrl.withTrailingSlash(), connectionPool, freshAccessToken, customInterceptor = requestInterceptor)
      .addConverterFactory(converterFactory)
      .build()
      .create(Endpoints::class.java)
  }

  abstract fun getFreshAccessToken(userAccount: UserAccount?, accessToken: String?): String?

  fun getOAuthPattern(suffix: String = """\?$CODE_ARGUMENT=[\w+,.-]*\&$STATE=[\w+,.-]*"""): Pattern {
    return "^.*$oAuthServicePath$suffix".toPattern()
  }

  fun getServicePattern(suffix: String): Pattern = "^.*$serviceName$suffix".toPattern()

  open fun isLoggedIn(): Boolean = account != null

  companion object {
    @JvmStatic
    @NonNls
    protected val OAUTH_SUFFIX: String = "oauth"
    const val STATE: String = "state"

    /**
     * Retrofit builder needs url to be with trailing slash
     */
    protected fun String.withTrailingSlash(): String = if (!endsWith('/')) "$this/" else this
  }
}