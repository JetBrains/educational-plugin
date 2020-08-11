package com.jetbrains.edu.learning.twitter

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.text.StringUtil
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.messages.EduCoreErrorBundle
import com.jetbrains.edu.learning.twitter.ui.createTwitterDialogUI
import org.apache.http.HttpStatus
import twitter4j.StatusUpdate
import twitter4j.Twitter
import twitter4j.TwitterException
import twitter4j.TwitterFactory
import twitter4j.auth.AccessToken
import twitter4j.conf.ConfigurationBuilder
import java.io.IOException
import java.nio.file.Path

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
      val dialog = createTwitterDialogUI(project) { configurator.getTweetDialogPanel(task, it) }
      if (dialog.showAndGet()) {
        val settings = TwitterSettings.getInstance()
        try {
          val isAuthorized = settings.accessToken.isNotEmpty()
          val twitter = twitter
          val info = TweetInfo(dialog.message, configurator.getImagePath(task))

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
            EduCoreErrorBundle.message("failed.to.authorize")
          } else {
            EduCoreErrorBundle.message("failed.to.update.status")
          }
          Messages.showErrorDialog(project, message, EduCoreErrorBundle.message("twitter.failed.to.tweet"))
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
    val mediaPath = info.mediaPath
    if (mediaPath != null) {
      update.media(mediaPath.toFile())
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
    return Messages.showInputDialog(project, EduCoreBundle.message("twitter.enter.pin"), EduCoreBundle.message("twitter.authorization"), null, "", TwitterPinValidator())
  }

  private class TweetInfo(
    val message: String,
    val mediaPath: Path?
  )

  private class TwitterPinValidator : InputValidatorEx {
    override fun getErrorText(inputString: String): String? {
      val input = inputString.trim()
      return when {
        input.isEmpty() -> EduCoreBundle.message("twitter.validation.empty.pin")
        !isNumeric(input) -> EduCoreBundle.message("twitter.validation.not.numeric.pin")
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
