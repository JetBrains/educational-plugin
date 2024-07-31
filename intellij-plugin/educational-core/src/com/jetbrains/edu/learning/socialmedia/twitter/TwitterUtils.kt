package com.jetbrains.edu.learning.socialmedia.twitter

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.BrowserUtil
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType.INFORMATION
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task.Backgroundable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.NlsSafe
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.edu.learning.socialmedia.suggestToPostDialog.SuggestToPostDialogUI
import com.jetbrains.edu.learning.socialmedia.twitter.dialog.createTwitterDialogUI
import twitter4j.*
import twitter4j.auth.AccessToken
import twitter4j.auth.RequestToken
import twitter4j.conf.ConfigurationBuilder
import java.io.IOException
import java.net.HttpURLConnection
import java.nio.file.Path

object TwitterUtils {
  private val LOG = Logger.getInstance(TwitterUtils::class.java)
  private const val REQUEST_PIN_CALLBACK = "oob"

  @Suppress("UnstableApiUsage")
  @NlsSafe
  private const val SERVICE_DISPLAY_NAME: String = "JetBrains Academy Twitter Integration"

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
      val dialog = createTwitterDialogUI(project) { configurator.getPostDialogPanel(task, imagePath, it) }
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
    val mediaId = if (mediaPath != null) arrayOf(twitter.uploadMedia(mediaPath.toFile()).mediaId) else emptyArray<Long>()

    val tweet = twitter.v2.createTweet(text = info.message, mediaIds = mediaId)

    EduNotificationManager
      .create(INFORMATION, EduCoreBundle.message("twitter.success.title"), EduCoreBundle.message("twitter.tweet.posted"))
      .addAction(NotificationAction.createSimpleExpiring(EduCoreBundle.message("twitter.open.in.browser")) {
        EduBrowser.getInstance().browse("https://twitter.com/anyuser/status/${tweet.id}")
      })
      .notify(null)
  }

  /**
   * Returns true if a user finished authorization successfully, false otherwise
   */
  @Throws(TwitterException::class)
  private fun authorize(project: Project, twitter: Twitter): Boolean {
    checkIsBackgroundThread()
    val requestToken = twitter.getOAuthRequestToken(REQUEST_PIN_CALLBACK)
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
    return Messages.showInputDialog(
      project, EduCoreBundle.message("twitter.enter.pin"), EduCoreBundle.message("twitter.authorization"),
      null, "", NumericInputValidator(
        EduCoreBundle.message("twitter.validation.empty.pin"),
        EduCoreBundle.message("twitter.validation.not.numeric.pin")
      )
    )
  }

  private class TweetInfo(
    val message: String,
    val mediaPath: Path?
  )

  private class TweetingBackgroundableTask(
    project: Project,
    private val dialog: SuggestToPostDialogUI,
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
      }
      else {
        EduCoreBundle.message("error.failed.to.update.status")
      }
      Messages.showErrorDialog(project, message, EduCoreBundle.message("twitter.error.failed.to.tweet"))
    }
  }
}
