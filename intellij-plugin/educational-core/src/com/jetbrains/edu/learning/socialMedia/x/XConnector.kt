package com.jetbrains.edu.learning.socialMedia.x

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.util.ProgressIndicatorUtils
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.jetbrains.edu.learning.api.EduOAuthCodeFlowConnector
import com.jetbrains.edu.learning.authUtils.ConnectorUtils
import com.jetbrains.edu.learning.network.executeHandlingExceptions
import com.jetbrains.edu.learning.socialMedia.x.api.*
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.apache.http.client.utils.URIBuilder
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

@Service(Service.Level.APP)
class XConnector : EduOAuthCodeFlowConnector<XAccount, XUserInfo> {
  override val baseUrl: String
  override val clientId: String
  private val authBaseUrl: String

  // Documentation recommends using 127.0.0.1 instead of localhost
  // https://docs.x.com/resources/fundamentals/developer-apps#things-to-keep-in-mind
  override val redirectHost: String = "127.0.0.1"

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
    val tweet = if (imagePath != null) {
      val mediaId = uploadMedia(imagePath) ?: return null
      Tweet(message, Media(listOf(mediaId)))
    }
    else {
      Tweet(message, Media(listOf()))
    }

    return getEndpoints<XV2>()
      .postTweet(tweet)
      .executeHandlingExceptions(omitErrors = true)
      ?.body()
  }

  private fun uploadMedia(imagePath: Path): String? {
    val endpoints = getEndpoints<XV2>()

    val mediaType = imagePath.mediaType()
    val media = imagePath.toFile().asRequestBody(mediaType)

    // Supposed workflow for media uploading - https://docs.x.com/x-api/media/quickstart/media-upload-chunked

    // 1. Init media loading and receiving media id
    val initResult = endpoints.uploadMedia(
      mapOf(
        "command" to UploadCommand.INIT,
        "media_type" to mediaType,
        "media_category" to "tweet_gif", // Hardcoded value. Need to be dynamic if we decide to use new media types
        "total_bytes" to media.contentLength()
      ).convertToRequestBodies()
    ).executeHandlingExceptions(omitErrors = true)?.body() ?: return null

    val mediaId = initResult.data.id

    // 2. Upload media chunks.
    // Right now we do it with a single chunk
    endpoints.uploadMedia(
      mapOf(
        "command" to UploadCommand.APPEND,
        "media_id" to mediaId,
        "segment_index" to 0, // Do we need to split media data into several chunks?
        "media" to media
      ).convertToRequestBodies()
    ).executeHandlingExceptions(omitErrors = true) ?: return null

    // 3. Finalize uploading and receiving media status
    var uploadInfo = endpoints.uploadMedia(
      mapOf(
        "command" to UploadCommand.FINALIZE,
        "media_id" to mediaId,
      ).convertToRequestBodies()
    ).executeHandlingExceptions(omitErrors = true)?.body() ?: return null

    // 4. Checking uploading status
    var totalWaitingDuration = 0L
    while (uploadInfo.data.processingInfo != null) { // the absense of `processingInfo` means that media was uploaded successfully
      when (uploadInfo.data.processingInfo.state) {
        PendingState.PENDING, PendingState.IN_PROGRESS -> {
          if (totalWaitingDuration > IMAGE_PROCESSING_WAITING_TIMEOUT) return null

          val waitingTime = TimeUnit.SECONDS.toMillis(uploadInfo.data.processingInfo.checkAfterSecs)
          totalWaitingDuration += waitingTime

          @Suppress("UsagesOfObsoleteApi")
          ProgressIndicatorUtils.awaitWithCheckCanceled(waitingTime)
          uploadInfo = endpoints.mediaUploadStatus(mediaId).executeHandlingExceptions(omitErrors = true)?.body() ?: return null
        }
        PendingState.SUCCEEDED -> break
        PendingState.FAILED -> return null
      }
    }

    return mediaId
  }

  /**
   * Converts all values to [RequestBody].
   * If value is [RequestBody], does nothing.
   * Otherwise, converts values to string with text/plain media type
   */
  private fun Map<String, Any>.convertToRequestBodies(): Map<String, RequestBody> = mapValues { (_, value) ->
    value as? RequestBody ?: value.toString().toRequestBody(TEXT_PLAIN)
  }

  /**
   * In case of any issues with content type detection, `image/gif` will be used as default media type.
   * Maybe be a reason for a wrong media type if we start using anything except gif images for tweets.
   */
  private fun Path.mediaType(): MediaType {
    return runCatching { Files.probeContentType(this).toMediaType() }.getOrElse { IMAGE_GIF }
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

    private val TEXT_PLAIN: MediaType = "text/plain".toMediaType()
    private val IMAGE_GIF: MediaType = "image/gif".toMediaType()

    private val IMAGE_PROCESSING_WAITING_TIMEOUT = TimeUnit.SECONDS.toMillis(10)

    fun getInstance(): XConnector = service()
  }
}
