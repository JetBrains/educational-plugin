package com.jetbrains.edu.learning.twitter

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.twitter.ui.TwitterDialog
import org.apache.http.HttpStatus
import twitter4j.StatusUpdate
import twitter4j.Twitter
import twitter4j.TwitterException
import twitter4j.TwitterFactory
import twitter4j.auth.AccessToken
import twitter4j.conf.ConfigurationBuilder
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

object TwitterUtils {
  private val LOG = Logger.getInstance(TwitterUtils::class.java)

  /**
   * Set consumer key and secret.
   * @return Twitter instance with consumer key and secret set.
   */
  val twitter: Twitter
    get() {
      val configuration = ConfigurationBuilder()
        .setOAuthConsumerKey(TwitterBundle.message("twitterConsumerKey"))
        .setOAuthConsumerSecret(TwitterBundle.message("twitterConsumerSecret"))
        .build()
      return TwitterFactory(configuration).instance
    }

  @JvmStatic
  fun createTwitterDialogAndShow(project: Project, configurator: TwitterPluginConfigurator, task: Task) {
    ApplicationManager.getApplication().invokeLater {
      val panel = configurator.getTweetDialogPanel(task)
      val dialog = TwitterDialog(project, panel)
      if (dialog.showAndGet()) {
        val settings = TwitterSettings.getInstance()
        try {
          val isAuthorized = settings.accessToken.isNotEmpty()
          val twitter = twitter
          val info = TweetInfo(panel.message, configurator, task)

          if (!isAuthorized) {
            authorize(project, twitter)
          }
          else {
            twitter.oAuthAccessToken = AccessToken(settings.accessToken, settings.tokenSecret)
          }
          updateStatus(twitter, info)
        }
        catch (e: Exception) {
          LOG.warn(e)
          val message = if (e is TwitterException && e.statusCode == HttpStatus.SC_UNAUTHORIZED) {
            "Failed to authorize"
          } else {
            "Status wasn't updated. Please, check internet connection and try again"
          }
          Messages.showErrorDialog(project, message, "Failed to Tweet")
        }
      }
    }
  }
  /**
   * Post on twitter media and text from panel.
   * As a result of succeeded tweet twitter website is opened in default browser.
   */
  @Throws(IOException::class, TwitterException::class)
  private fun updateStatus(twitter: Twitter, info: TweetInfo) {
    val update = StatusUpdate(info.message)
    val mediaSource = info.mediaSource
    if (mediaSource != null) {
      val imageFile = FileUtil.createTempFile("twitter_media", info.mediaExtension)
      FileUtil.copy(mediaSource, FileOutputStream(imageFile))
      update.media(imageFile)
    }
    twitter.updateStatus(update)
    BrowserUtil.browse("https://twitter.com/")
  }

  @Throws(TwitterException::class)
  private fun authorize(project: Project, twitter: Twitter) {
    val requestToken = twitter.oAuthRequestToken
    BrowserUtil.browse(requestToken.authorizationURL)
    val pin = createAndShowPinDialog(project) ?: return
    val token = twitter.getOAuthAccessToken(requestToken, pin)
    val settings = TwitterSettings.getInstance()
    settings.accessToken = token.token
    settings.tokenSecret = token.tokenSecret
  }

  private fun createAndShowPinDialog(project: Project): String? {
    return Messages.showInputDialog(project, "Enter Twitter PIN:", "Twitter Authorization", null, "", TwitterPinValidator())
  }

  private class TweetInfo(
    val message: String,
    private val configurator: TwitterPluginConfigurator,
    private val task: Task
  ) {
    val mediaExtension: String get() =
      configurator.getMediaExtension(task)

    val mediaSource: InputStream? get() =
      configurator.javaClass.getResourceAsStream(configurator.getImageResourcePath(task))
  }

  private class TwitterPinValidator : InputValidatorEx {
    override fun getErrorText(inputString: String): String? {
      val input = inputString.trim()
      return when {
        input.isEmpty() -> "PIN shouldn't be empty"
        !isNumeric(input) -> "PIN should be numeric"
        else -> null
      }
    }

    override fun checkInput(inputString: String): Boolean {
      return getErrorText(inputString) == null
    }

    override fun canClose(inputString: String): Boolean = true

    private fun isNumeric(string: String): Boolean {
      return string.all { StringUtil.isDecimalDigit(it) }
    }
  }
}
