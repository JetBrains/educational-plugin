package com.jetbrains.edu.learning.checkio.connectors

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.intellij.ide.BrowserUtil
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.annotations.Transient
import com.jetbrains.edu.learning.EduLogInListener
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.api.ConnectorUtils
import com.jetbrains.edu.learning.authUtils.CustomAuthorizationServer
import com.jetbrains.edu.learning.authUtils.OAuthUtils.GrantType.AUTHORIZATION_CODE
import com.jetbrains.edu.learning.authUtils.OAuthUtils.GrantType.REFRESH_TOKEN
import com.jetbrains.edu.learning.authUtils.TokenInfo
import com.jetbrains.edu.learning.checkio.account.CheckiOAccount
import com.jetbrains.edu.learning.checkio.api.CheckiOOAuthEndpoint
import com.jetbrains.edu.learning.checkio.api.exceptions.ApiException
import com.jetbrains.edu.learning.checkio.api.exceptions.NetworkException
import com.jetbrains.edu.learning.checkio.api.executeHandlingCheckiOExceptions
import com.jetbrains.edu.learning.checkio.exceptions.CheckiOLoginRequiredException
import com.jetbrains.edu.learning.checkio.notifications.CheckiONotifications.error
import com.jetbrains.edu.learning.checkio.utils.CheckiONames
import com.jetbrains.edu.learning.checkio.utils.CheckiONames.CHECKIO_OAUTH_REDIRECT_HOST
import com.jetbrains.edu.learning.createRetrofitBuilder
import com.jetbrains.edu.learning.messages.EduCoreBundle.message
import okhttp3.ConnectionPool
import org.apache.http.client.utils.URIBuilder
import org.jetbrains.builtInWebServer.BuiltInServerOptions
import org.jetbrains.ide.BuiltInServerManager
import retrofit2.converter.jackson.JacksonConverterFactory
import java.net.URI

abstract class CheckiOOAuthConnector protected constructor(private val clientId: String, private val clientSecret: String) {

  @get:Transient
  @set:Transient
  abstract var account: CheckiOAccount?

  protected abstract val oAuthServicePath: String

  protected abstract val platformName: String

  private var authorizationBusConnection = ApplicationManager.getApplication().messageBus.connect()
  private val authorizationService: CheckiOOAuthEndpoint by lazy { authorizationService() }

  private val connectionPool: ConnectionPool = ConnectionPool()
  private val converterFactory: JacksonConverterFactory
  private val objectMapper: ObjectMapper

  private val currentPort: Int
    get() = BuiltInServerManager.getInstance().port

  private val customServer: CustomAuthorizationServer
    get() {
      val startedServer = CustomAuthorizationServer.getServerIfStarted(platformName)
      return startedServer ?: createCustomServer()
    }

  private val redirectUri: String
    get() = """$CHECKIO_OAUTH_REDIRECT_HOST:${currentPort}$oAuthServicePath"""

  init {
    require(SPACES_SYMBOLS_REGEX !in clientId && SPACES_SYMBOLS_REGEX !in clientSecret) {
      "Client properties are not provided"
    }

    val module = SimpleModule()
    objectMapper = ConnectorUtils.createRegisteredMapper(module)
    converterFactory = JacksonConverterFactory.create(objectMapper)
  }

  private fun authorizationService(): CheckiOOAuthEndpoint =
    createRetrofitBuilder(CheckiONames.CHECKIO_OAUTH_HOST, connectionPool)
      .addConverterFactory(converterFactory)
      .build()
      .create(CheckiOOAuthEndpoint::class.java)

  private fun getTokens(code: String, redirectUri: String): TokenInfo {
    return authorizationService.getTokens(AUTHORIZATION_CODE, clientSecret, clientId, code, redirectUri).executeHandlingCheckiOExceptions()
  }

  private fun refreshTokens(refreshToken: String): TokenInfo {
    return authorizationService.refreshTokens(REFRESH_TOKEN, clientSecret, clientId, refreshToken).executeHandlingCheckiOExceptions()
  }

  open fun getAccessToken(): String {
    val currentAccount = account ?: throw CheckiOLoginRequiredException()
    if (!currentAccount.isUpToDate()) {
      val refreshToken = currentAccount.getRefreshToken() ?: error("Cannot get refresh token")
      val newTokens = refreshTokens(refreshToken)
      currentAccount.tokenExpiresIn = newTokens.expiresIn
      currentAccount.saveTokens(newTokens)
    }
    return currentAccount.getAccessToken() ?: error("Cannot get access token")
  }

  fun doAuthorize(vararg postLoginActions: Runnable) {
    try {
      val handlerUri = getOAuthHandlerUri()
      val oauthLink = getOauthLink(handlerUri)
      createAuthorizationListener(*postLoginActions)
      BrowserUtil.browse(oauthLink)
    }
    catch (e: Exception) {
      // IOException is thrown when there're no available ports, in some cases restarting can fix this
      Notifications.Bus.notify(error(
        message("notification.title.failed.to.authorize"),
        null,
        message("notification.content.try.to.restart.ide.and.log.in.again")
      ))
    }
  }

  private fun getOAuthHandlerUri(): String {
    if (EduUtils.isAndroidStudio()) {
      return customServer.handlingUri
    }
    val defaultPort = BuiltInServerOptions.DEFAULT_PORT
    // 20 port range comes from org.jetbrains.ide.BuiltInServerManagerImplKt.PORTS_COUNT
    if (currentPort !in defaultPort..defaultPort + 20) {
      error("No ports available")
    }
    return redirectUri
  }

  private fun getOauthLink(oauthRedirectUri: String): URI {
    return URIBuilder(CheckiONames.CHECKIO_OAUTH_URL + "/")
      .addParameter("redirect_uri", oauthRedirectUri)
      .addParameter("response_type", "code")
      .addParameter("client_id", clientId)
      .build()
  }

  private fun createAuthorizationListener(vararg postLoginActions: Runnable) {
    authorizationBusConnection.disconnect()
    authorizationBusConnection = ApplicationManager.getApplication().messageBus.connect()
    authorizationBusConnection.subscribe(authorizationTopic, object : EduLogInListener {
      override fun userLoggedIn() {
        for (action in postLoginActions) {
          action.run()
        }
      }

      override fun userLoggedOut() {}
    })
  }

  private fun createCustomServer(): CustomAuthorizationServer {
    return CustomAuthorizationServer.create(platformName, oAuthServicePath) { code: String, handlingPath: String ->
      codeHandler(code, handlingPath)
    }
  }

  // In case of built-in server
  fun codeHandler(code: String): String? {
    return codeHandler(code, redirectUri)
  }

  // In case of Android Studio
  @Synchronized
  private fun codeHandler(code: String, handlingPath: String): String? {
    return try {
      if (account != null) {
        ApplicationManager.getApplication().messageBus.syncPublisher(authorizationTopic).userLoggedIn()
        return "You're logged in already"
      }
      val tokens = getTokens(code, handlingPath)
      val userInfo = authorizationService.getUserInfo(tokens.accessToken).executeHandlingCheckiOExceptions()
      account = CheckiOAccount(userInfo, tokens)
      ApplicationManager.getApplication().messageBus.syncPublisher(authorizationTopic).userLoggedIn()
      null
    }
    catch (e: NetworkException) {
      "Connection failed"
    }
    catch (e: ApiException) {
      "Couldn't get user info"
    }
  }

  companion object {
    private val SPACES_SYMBOLS_REGEX: Regex = "[\\p{javaWhitespace}]+".toRegex()

    @JvmStatic
    val authorizationTopic: Topic<EduLogInListener> = Topic.create("Edu.checkioUserLoggedIn", EduLogInListener::class.java)
  }
}