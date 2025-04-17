package com.jetbrains.edu.learning.socialMedia.x

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.jetbrains.edu.learning.api.EduOAuthCodeFlowConnector
import com.jetbrains.edu.learning.authUtils.ConnectorUtils
import com.jetbrains.edu.learning.network.executeHandlingExceptions
import com.jetbrains.edu.learning.socialMedia.x.api.Media
import com.jetbrains.edu.learning.socialMedia.x.api.Tweet
import com.jetbrains.edu.learning.socialMedia.x.api.TweetResponse
import com.jetbrains.edu.learning.socialMedia.x.api.XV2
import org.apache.http.client.utils.URIBuilder
import java.nio.file.Path

@Service(Service.Level.APP)
class XConnector : EduOAuthCodeFlowConnector<XAccount, XUserInfo> {
  override val baseUrl: String
  override val clientId: String
  private val authBaseUrl: String

  // https://docs.x.com/resources/fundamentals/authentication/oauth-2-0/authorization-code
  override val authorizationUrlBuilder: URIBuilder
    get() = URIBuilder(authBaseUrl)
      .setPath("/i/oauth2/authorize")
      .addParameter("client_id", clientId)
      .addParameter("redirect_uri", getRedirectUri())
      .addParameter("response_type", "code")
      .addParameter("scope", SCOPES.joinToString(separator = " "))

  // https://docs.x.com/resources/fundamentals/authentication/oauth-2-0/authorization-code#refresh-tokens
  override val baseOAuthTokenUrl: String
    get() = "/2/oauth2/token"

  override val objectMapper: ObjectMapper by lazy {
    ConnectorUtils.createRegisteredMapper(kotlinModule())
  }

  override val platformName: String = XUtils.PLATFORM_NAME

  override var account: XAccount? by XSettings.getInstance()::account

  // TODO: replace empty `clientId` with actual one
  @Suppress("unused") // used by the platform
  constructor() : this(authBaseUrl = AUTH_BASE_URL, baseUrl = BASE_URL, clientId = "")

  constructor(authBaseUrl: String, baseUrl: String, clientId: String) : super() {
    this.authBaseUrl = authBaseUrl
    this.baseUrl = baseUrl
    this.clientId = clientId
  }

  // TODO: refactor `EduOAuthCodeFlowConnector`.
  //  Currently, `getUserInfo` is not used in this connector because its design and it's implemented only because of base class
  override fun getUserInfo(account: XAccount, accessToken: String?): XUserInfo? {
    return getEndpoints<XV2>(account, accessToken).userInfo()
  }

  @Synchronized
  override fun login(code: String): Boolean {
    val tokenInfo = retrieveLoginToken(code, getRedirectUri()) ?: return false

    val userInfo = getEndpoints<XV2>(null, tokenInfo.accessToken).userInfo() ?: return false
    val account = XAccount(userInfo, tokenInfo.expiresIn)
    account.saveTokens(tokenInfo)

    this.account = account
    notifyUserLoggedIn()

    return true
  }

  private fun XV2.userInfo(): XUserInfo? {
    val data = usersMe().executeHandlingExceptions()?.body()?.data ?: return null
    return XUserInfo(userName = data.username, name = data.name)
  }

  @RequiresBackgroundThread
  fun tweet(message: String, imagePath: Path?): TweetResponse? {
    val tweet = Tweet(message, Media(listOf()))

    return getEndpoints<XV2>()
      .postTweet(tweet)
      .executeHandlingExceptions(omitErrors = true)
      ?.body()
  }

  companion object {
    const val BASE_URL = "https://api.x.com"
    const val AUTH_BASE_URL = "https://x.com"

    // https://docs.x.com/resources/fundamentals/authentication/oauth-2-0/authorization-code#scopes
    private val SCOPES = listOf(
      "offline.access", // to get a refresh token (`/2/oauth2/token` call)
      "users.read",     // to get user info (`/2/users/me` call)
      "tweet.read",     // also to get user info. For some reason, `users.read` is not enough
      "tweet.write",    // to post new tweets (`/2/tweets` call)
      "media.write"     // to upload media (gifs in our case) for tweets (`/2/media/upload` calls)
    )

    fun getInstance(): XConnector = service()
  }
}
