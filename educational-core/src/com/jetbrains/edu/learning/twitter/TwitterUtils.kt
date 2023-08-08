package com.jetbrains.edu.learning.twitter

import com.fasterxml.jackson.databind.json.JsonMapper
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.BrowserUtil
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task.Backgroundable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.twitter.ui.TwitterDialogUI
import com.jetbrains.edu.learning.twitter.ui.createTwitterDialogUI
import okhttp3.ConnectionPool
import retrofit2.converter.jackson.JacksonConverterFactory
import twitter4j.*
import twitter4j.auth.AccessToken
import twitter4j.auth.RequestToken
import twitter4j.conf.ConfigurationBuilder
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.nio.file.Path
import kotlin.io.path.exists

object TwitterUtils {
  private val LOG = Logger.getInstance(TwitterUtils::class.java)

  @Suppress("UnstableApiUsage")
  @NlsSafe
  private const val SERVICE_DISPLAY_NAME: String = "EduTools Twitter Integration"

  private val connectionPool: ConnectionPool = ConnectionPool()
  private val converterFactory: JacksonConverterFactory = JacksonConverterFactory.create(JsonMapper())

  /**
   * Set consumer key and secret.
   * @return Twitter instance with consumer key and secret set.
   */
  val twitter: Twitter
    get() {
      val configuration = ConfigurationBuilder()
        .setOAuthConsumerKey(TwitterBundle.value("twitterConsumerKey"))
        .setOAuthConsumerSecret(TwitterBundle.value("twitterConsumerSecret"))
        .build()
      return TwitterFactory(configuration).instance
    }

  fun createTwitterDialogAndShow(project: Project, configurator: TwitterPluginConfigurator, task: Task) {
    project.invokeLater {
      val imagePath = configurator.getImagePath(task)
      val dialog = createTwitterDialogUI(project) { configurator.getTweetDialogPanel(task, imagePath, it) }
      if (dialog.showAndGet()) {
        ProgressManager.getInstance().run(TweetingBackgroundableTask(project, dialog, imagePath))
      }
    }
  }

  private fun getToken(userId: String): RequestToken? {
    val tokens = PasswordSafe.instance.get(credentialAttributes(userId)) ?: return null
    val token = tokens.userName
    val tokenSecret = tokens.getPasswordAsString()
    if (token == null || tokenSecret == null) {
      return null
    }
    return RequestToken(token, tokenSecret)
  }

  /**
   * Post on twitter media and text from panel.
   * As a result of succeeded tweet twitter website is opened in default browser.
   */
  @Throws(IOException::class, TwitterException::class)
  private fun updateStatus(twitter: Twitter, info: TweetInfo) {
    checkIsBackgroundThread()

    val mediaPath = info.mediaPath
    val mediaId = if (mediaPath != null) {
      twitter.uploadMedia(mediaPath.toFile()).mediaId
    }
    else {
      null
    }

    val tweet = Tweet(info.message, Media(listOfNotNull(mediaId)))
    twitter.postTweet(tweet)

    EduBrowser.getInstance().browse("https://twitter.com/")
  }

  @Throws(IOException::class)
  private fun Twitter.postTweet(tweet: Tweet) {
    val response = v2().postTweet(tweet).execute()
    if (!response.isSuccessful) {
      throw IOException(response.errorBody()?.string() ?: "${response.code()} failed to create tweet")
    }
  }

  private fun Twitter.v2(): TwitterV2 {
    val authHeaderValue = constructAuthHeaderValue()
    return createRetrofitBuilder("https://api.twitter.com", connectionPool, authHeaderValue, authHeaderValue = null)
      .addConverterFactory(converterFactory)
      .build()
      .create(TwitterV2::class.java)
  }

  /**
   * Constructs proper value for `Authorization` header based on OAuth 1.0a
   *
   * See https://developer.twitter.com/en/docs/authentication/oauth-1-0a
   */
  private fun Twitter.constructAuthHeaderValue(): String {
    val authorization = authorization
    return authorization.getAuthorizationHeader(
      HttpRequest(RequestMethod.POST, "https://api.twitter.com/2/tweets", null, authorization, emptyMap())
    )
  }

  /**
   * Uploads media file using https://upload.twitter.com/1.1/media/upload.json endpoint
   */
  private fun Twitter.uploadMedia(file: File): UploadedMedia {
    val client = HttpClientFactory.getInstance(configuration.httpClientConfiguration)
    val param = HttpParameter("media", file)
    val response = client.post("https://upload.twitter.com/1.1/media/upload.json", arrayOf(param), authorization, null)
    return UploadedMedia.fromResponse(response)
  }

  private class UploadedMedia(val mediaId: String) {
    companion object {
      fun fromResponse(response: HttpResponse): UploadedMedia {
        val json = response.asJSONObject()
        val mediaId = json.getString("media_id_string")
        return UploadedMedia(mediaId)
      }
    }
  }

  /**
   * Returns true if a user finished authorization successfully, false otherwise
   */
  @Throws(TwitterException::class)
  private fun authorize(project: Project, twitter: Twitter): Boolean {
    checkIsBackgroundThread()
    val requestToken = twitter.oAuthRequestToken
    BrowserUtil.browse(requestToken.authorizationURL)
    val pin = invokeAndWaitIfNeeded { createAndShowPinDialog(project) } ?: return false
    ProgressManager.checkCanceled()
    val token = twitter.getOAuthAccessToken(requestToken, pin)
    ProgressManager.checkCanceled()
    invokeAndWaitIfNeeded {
      val credentialAttributes = credentialAttributes(TwitterSettings.getInstance().userId)
      PasswordSafe.instance.set(credentialAttributes, Credentials(token.token, token.tokenSecret))
    }
    return true
  }

  private fun credentialAttributes(userId: String) =
    CredentialAttributes(generateServiceName(SERVICE_DISPLAY_NAME, userId))

  private fun createAndShowPinDialog(project: Project): String? {
    return Messages.showInputDialog(project, EduCoreBundle.message("twitter.enter.pin"), EduCoreBundle.message("twitter.authorization"),
                                    null, "", NumericInputValidator(EduCoreBundle.message("twitter.validation.empty.pin"),
                                                                    EduCoreBundle.message("twitter.validation.not.numeric.pin")))
  }

  private class TweetInfo(
    val message: String,
    val mediaPath: Path?
  )

  fun pluginRelativePath(path: String): Path? {
    require(!FileUtil.isAbsolute(path)) { "`$path` shouldn't be absolute" }

    return PluginManagerCore.getPlugin(PluginId.getId(EduNames.PLUGIN_ID))
      ?.pluginPath
      ?.resolve(path)
      ?.takeIf { it.exists() }
  }

  private class TweetingBackgroundableTask(
    project: Project,
    private val dialog: TwitterDialogUI,
    private val imagePath: Path?
    ) : Backgroundable(project, EduCoreBundle.message("twitter.loading.posting"), true) {

    override fun run(indicator: ProgressIndicator) {
      val token = getToken(TwitterSettings.getInstance().userId)
      val twitterInstance = twitter
      if (token == null) {
        if (!authorize(project, twitterInstance)) return
      }
      else {
        twitterInstance.oAuthAccessToken = AccessToken(token.token, token.tokenSecret)
      }

      ProgressManager.checkCanceled()
      updateStatus(twitterInstance, TweetInfo(dialog.message, imagePath))
    }

    override fun onThrowable(error: Throwable) {
      LOG.warn(error)
      val message = if (error is TwitterException && error.statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
        EduCoreBundle.message("error.failed.to.authorize")
      } else {
        EduCoreBundle.message("error.failed.to.update.status")
      }
      Messages.showErrorDialog(project, message, EduCoreBundle.message("twitter.error.failed.to.tweet"))
    }
  }
}
