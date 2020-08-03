package com.jetbrains.edu.learning.twitter

import com.intellij.ide.BrowserUtil
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task.Backgroundable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.io.exists
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.checkIsBackgroundThread
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.messages.EduCoreErrorBundle
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
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
        val isAuthorized = settings.accessToken.isNotEmpty()
        val twitter = twitter
        val info = TweetInfo(dialog.message, configurator.getImagePath(task))

        ProgressManager.getInstance().run(object : Backgroundable(project, EduCoreBundle.message("twitter.loading.posting"), true) {
          override fun run(indicator: ProgressIndicator) {
            if (!isAuthorized) {
              if (!authorize(project, twitter)) return
            }
            else {
              twitter.oAuthAccessToken = AccessToken(settings.accessToken, settings.tokenSecret)
            }

            ProgressManager.checkCanceled()
            updateStatus(twitter, info)
            EduCounterUsageCollector.twitterAchievementPosted(task.course)
          }

          override fun onThrowable(error: Throwable) {
            LOG.warn(error)
            val message = if (error is TwitterException && error.statusCode == HttpStatus.SC_UNAUTHORIZED) {
              EduCoreErrorBundle.message("error.failed.to.authorize")
            }
            else {
              EduCoreErrorBundle.message("error.failed.to.update.status")
            }
            Messages.showErrorDialog(project, message, EduCoreErrorBundle.message("twitter.error.failed.to.tweet"))
          }
        })
      }
    }
  }
  /**
   * Post on twitter media and text from panel.
   * As a result of succeeded tweet twitter website is opened in default browser.
   */
  @Throws(IOException::class, TwitterException::class)
  private fun updateStatus(twitter: Twitter, info: TweetInfo) {
    checkIsBackgroundThread()
    val update = StatusUpdate(info.message)
    val mediaPath = info.mediaPath
    if (mediaPath != null) {
      update.media(mediaPath.toFile())
    }
    twitter.updateStatus(update)
    BrowserUtil.browse("https://twitter.com/")
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
      val settings = TwitterSettings.getInstance()
      settings.accessToken = token.token
      settings.tokenSecret = token.tokenSecret
    }
    return true
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

  fun pluginRelativePath(path: String): Path? {
    require(!FileUtil.isAbsolute(path)) { "`$path` shouldn't be absolute" }

     // BACKCOMPAT: 2019.3. Use `pluginPath` instead of `path?.toPath()`
    @Suppress("DEPRECATION")
    return PluginManagerCore.getPlugin(PluginId.getId(EduNames.PLUGIN_ID))
      ?.path
      ?.toPath()
      ?.resolve(path)
      ?.takeIf { it.exists() }
  }
}
