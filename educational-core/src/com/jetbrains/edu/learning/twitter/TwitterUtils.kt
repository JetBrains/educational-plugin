package com.jetbrains.edu.learning.twitter

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.BrowserUtil
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task.Backgroundable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.io.exists
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.NumericInputValidator
import com.jetbrains.edu.learning.checkIsBackgroundThread
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.twitter.ui.TwitterDialogUI
import com.jetbrains.edu.learning.twitter.ui.createTwitterDialogUI
import org.apache.http.HttpStatus
import twitter4j.StatusUpdate
import twitter4j.Twitter
import twitter4j.TwitterException
import twitter4j.TwitterFactory
import twitter4j.auth.AccessToken
import twitter4j.auth.RequestToken
import twitter4j.conf.ConfigurationBuilder
import java.io.IOException
import java.nio.file.Path

object TwitterUtils {
  private val LOG = Logger.getInstance(TwitterUtils::class.java)

  @Suppress("UnstableApiUsage")
  @NlsSafe
  private const val SERVICE_DISPLAY_NAME: String = "EduTools Twitter Integration"

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

  @JvmStatic
  fun createTwitterDialogAndShow(project: Project, configurator: TwitterPluginConfigurator, task: Task) {
    invokeLater {
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
    val update = StatusUpdate(info.message)
    val mediaPath = info.mediaPath
    if (mediaPath != null) {
      update.media(mediaPath.toFile())
    }
    twitter.updateStatus(update)
    EduBrowser.getInstance().browse("https://twitter.com/")
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
      val message = if (error is TwitterException && error.statusCode == HttpStatus.SC_UNAUTHORIZED) {
        EduCoreBundle.message("error.failed.to.authorize")
      } else {
        EduCoreBundle.message("error.failed.to.update.status")
      }
      Messages.showErrorDialog(project, message, EduCoreBundle.message("twitter.error.failed.to.tweet"))
    }
  }
}
