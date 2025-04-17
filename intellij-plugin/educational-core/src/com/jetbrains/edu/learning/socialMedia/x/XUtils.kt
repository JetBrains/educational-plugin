package com.jetbrains.edu.learning.socialMedia.x

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.BrowserUtil
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType.INFORMATION
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.registry.Registry
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.NumericInputValidator
import com.jetbrains.edu.learning.authUtils.requestFocus
import com.jetbrains.edu.learning.checkIsBackgroundThread
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.notification.EduNotificationManager
import twitter4j.Twitter
import twitter4j.TwitterException
import twitter4j.TwitterFactory
import twitter4j.auth.AccessToken
import twitter4j.auth.RequestToken
import twitter4j.conf.ConfigurationBuilder
import twitter4j.v2
import java.io.IOException
import java.nio.file.Path

object XUtils {
  private const val REQUEST_PIN_CALLBACK = "oob"

  @Suppress("UnstableApiUsage")
  @NlsSafe
  private const val SERVICE_DISPLAY_NAME: String = "JetBrains Academy X Integration"

  const val PLATFORM_NAME: String = "X"

  private val LOG = logger<XUtils>()

  /**
   * Set consumer key and secret.
   * @return Twitter instance with consumer key and secret set.
   */
  val twitter: Twitter
    get() {
      val configuration = ConfigurationBuilder()
        .setOAuthConsumerKey(XBundle.value("twitterConsumerKey"))
        .setOAuthConsumerSecret(XBundle.value("twitterConsumerSecret"))
        .build()
      return TwitterFactory(configuration).instance
    }

  val oauth2Enabled: Boolean get() = Registry.`is`("edu.socialMedia.x.oauth2")

  @RequiresBackgroundThread
  fun doPost(project: Project, message: String, imagePath: Path?) {
    try {
      if (oauth2Enabled) {
        doPostV2(project, message, imagePath)
      }
      else {
        doPostV1(project, message, imagePath)
      }
    }
    catch (e: Exception) {
      LOG.warn(e)
      showFailedToPostNotification(project)
    }
  }

  private fun doPostV1(project: Project, message: String, imagePath: Path?) {
    val token = getToken(XSettings.getInstance().userId)
    val twitterInstance = twitter
    if (token == null) {
      if (!authorize(project, twitterInstance)) return
    }
    else {
      twitterInstance.oAuthAccessToken = AccessToken(token.token, token.tokenSecret)
    }
    updateStatus(project, twitterInstance, TweetInfo(message, imagePath))
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
  private fun updateStatus(project: Project, twitter: Twitter, info: TweetInfo) {
    checkIsBackgroundThread()

    val mediaPath = info.mediaPath
    val mediaId = if (mediaPath != null) arrayOf(twitter.uploadMedia(mediaPath.toFile()).mediaId) else emptyArray<Long>()

    val tweet = twitter.v2.createTweet(text = info.message, mediaIds = mediaId)

    showSuccessNotification(project, tweet.id.toString())
  }

  /**
   * Returns true if a user finished authorization successfully, false otherwise
   */
  @Throws(TwitterException::class)
  private fun authorize(project: Project?, twitter: Twitter): Boolean {
    checkIsBackgroundThread()
    val requestToken = twitter.getOAuthRequestToken(REQUEST_PIN_CALLBACK)
    BrowserUtil.browse(requestToken.authorizationURL)
    val pin = invokeAndWaitIfNeeded { createAndShowPinDialog(project) } ?: return false
    ProgressManager.checkCanceled()
    val token = twitter.getOAuthAccessToken(requestToken, pin)
    ProgressManager.checkCanceled()
    val credentialAttributes = credentialAttributes(XSettings.getInstance().userId)
    PasswordSafe.instance.set(credentialAttributes, Credentials(token.token, token.tokenSecret))
    return true
  }

  private fun credentialAttributes(userId: String) =
    CredentialAttributes(generateServiceName(SERVICE_DISPLAY_NAME, userId))

  private fun createAndShowPinDialog(project: Project?): String? {
    return Messages.showInputDialog(
      project, EduCoreBundle.message("x.enter.pin"), EduCoreBundle.message("x.authorization"),
      null, "", NumericInputValidator(
        EduCoreBundle.message("x.validation.empty.pin"),
        EduCoreBundle.message("x.validation.not.numeric.pin")
      )
    )
  }

  private fun doPostV2(project: Project, message: String, imagePath: Path?) {
    val connector = XConnector.getInstance()
    if (connector.account == null) {
      connector.doAuthorize({
        runInEdt {
          requestFocus()
        }
        tweet(project, message, imagePath)
      })
    }
    else {
      tweet(project, message, imagePath)
    }
  }

  private fun tweet(project: Project, message: String, imagePath: Path?) {
    val response = try {
      XConnector.getInstance().tweet(message, imagePath)
    }
    catch (e: Exception) {
      LOG.warn(e)
      null
    }

    if (response?.data?.id != null) {
      showSuccessNotification(project, response.data.id)
    }
    else {
      showFailedToPostNotification(project)
    }
  }

  private fun showFailedToPostNotification(project: Project) {
    EduNotificationManager.showErrorNotification(
      project,
      EduCoreBundle.message("linkedin.error.failed.to.post"),
      EduCoreBundle.message("linkedin.error.failed.to.post")
    )
  }

  private fun showSuccessNotification(project: Project, postId: String) {
    EduNotificationManager
      .create(INFORMATION, EduCoreBundle.message("x.success.title"), EduCoreBundle.message("x.tweet.posted"))
      .addAction(NotificationAction.createSimpleExpiring(EduCoreBundle.message("x.open.in.browser")) {
        EduBrowser.getInstance().browse("https://x.com/anyuser/status/${postId}")
      })
      .notify(project)
  }

  private class TweetInfo(
    val message: String,
    val mediaPath: Path?
  )
}
