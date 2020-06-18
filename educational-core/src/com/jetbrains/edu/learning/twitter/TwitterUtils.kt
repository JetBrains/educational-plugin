package com.jetbrains.edu.learning.twitter

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle.message
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
import javax.swing.JComponent

object TwitterUtils {
  private val LOG = Logger.getInstance(TwitterUtils::class.java)

  /**
   * Set consumer key and secret.
   * @return Twitter instance with consumer key and secret set.
   */
  val twitter: Twitter
    get() {
      val configurationBuilder = ConfigurationBuilder()
      configurationBuilder.setOAuthConsumerKey(TwitterBundle.message("twitterConsumerKey"))
      configurationBuilder.setOAuthConsumerSecret(TwitterBundle.message("twitterConsumerSecret"))
      return TwitterFactory(configurationBuilder.build()).instance
    }

  /**
   * Set access token and token secret in Twitter instance
   */
  private fun setAuthInfoInTwitter(twitter: Twitter, accessToken: String, tokenSecret: String) {
    twitter.oAuthAccessToken = AccessToken(accessToken, tokenSecret)
  }

  @JvmStatic
  fun createTwitterDialogAndShow(project: Project, configurator: TwitterPluginConfigurator, task: Task) {
    ApplicationManager.getApplication().invokeLater {
      val doNotAskOption = createDoNotAskOption()
      val panel = configurator.getTweetDialogPanel(task)
      val wrapper = TwitterDialogWrapper(project, panel, doNotAskOption)
      wrapper.setDoNotAskOption(doNotAskOption)
      if (wrapper.showAndGet()) {
        val settings = TwitterSettings.getInstance()
        try {
          val isAuthorized = settings.accessToken.isNotEmpty()
          val twitter = twitter
          val info = TweetInfo(panel.message, configurator, task)

          if (!isAuthorized) {
            authorizeAndUpdateStatus(twitter, info)
          }
          else {
            setAuthInfoInTwitter(twitter, settings.accessToken, settings.tokenSecret)
            updateStatus(twitter, info)
          }
        }
        catch (e: Exception) {
          LOG.warn(e.message)
          Messages.showErrorDialog("Status wasn't updated. Please, check internet connection and try again", "Twitter")
        }
      }
    }
  }

  private fun createDoNotAskOption(): DialogWrapper.DoNotAskOption {
    return object : DialogWrapper.DoNotAskOption {
      override fun setToBeShown(toBeShown: Boolean, exitCode: Int) {
        if (exitCode == DialogWrapper.CANCEL_EXIT_CODE || exitCode == DialogWrapper.OK_EXIT_CODE) {
          TwitterSettings.getInstance().setAskToTweet(toBeShown)
        }
      }
      override fun isToBeShown(): Boolean = true
      override fun canBeHidden(): Boolean = true
      override fun shouldSaveOptionsOnCancel(): Boolean = true
      override fun getDoNotShowMessage(): String = message("twitter.dialog.do.not.ask")
    }
  }

  /**
   * Post on twitter media and text from panel
   */
  @Throws(IOException::class, TwitterException::class)
  private fun updateStatus(twitter: Twitter, info: TweetInfo) {
    val update = StatusUpdate(info.message)
    val e = info.mediaSource
    if (e != null) {
      val imageFile = FileUtil.createTempFile("twitter_media", info.mediaExtension)
      FileUtil.copy(e, FileOutputStream(imageFile))
      update.media(imageFile)
    }
    twitter.updateStatus(update)
    BrowserUtil.browse("https://twitter.com/")
  }

  /**
   * Show twitter dialog, asking user to tweet about his achievements. Post tweet with provided by panel
   * media and text.
   * As a result of succeeded tweet twitter website is opened in default browser.
   */
  @Throws(TwitterException::class)
  private fun authorizeAndUpdateStatus(twitter: Twitter, info: TweetInfo) {
    val requestToken = twitter.oAuthRequestToken
    BrowserUtil.browse(requestToken.authorizationURL)
    ApplicationManager.getApplication().invokeLater {
      val pin = createAndShowPinDialog()
      if (pin != null) {
        try {
          val token = twitter.getOAuthAccessToken(requestToken, pin)
          val settings = TwitterSettings.getInstance()
          settings.accessToken = token.token
          settings.tokenSecret = token.tokenSecret
          updateStatus(twitter, info)
        }
        catch (e: TwitterException) {
          if (e.statusCode == HttpStatus.SC_UNAUTHORIZED) {
            LOG.warn("Unable to get the access token.")
            LOG.warn(e.message)
          }
        }
        catch (e: IOException) {
          LOG.warn(e.message)
        }
      }
    }
  }

  private fun createAndShowPinDialog(): String? {
    return Messages.showInputDialog("Twitter PIN:", "Twitter Authorization", null, "", object : InputValidatorEx {
      override fun getErrorText(inputString: String): String? {
        val input = inputString.trim { it <= ' ' }
        return when {
          input.isEmpty() -> "PIN shouldn't be empty."
          !isNumeric(input) -> "PIN should be numeric."
          else -> null
        }
      }

      override fun checkInput(inputString: String): Boolean {
        return getErrorText(inputString) == null
      }

      override fun canClose(inputString: String): Boolean = true

      private fun isNumeric(string: String): Boolean {
        for (c in string.toCharArray()) {
          if (!StringUtil.isDecimalDigit(c)) {
            return false
          }
        }
        return true
      }
    })
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

  /**
   * Dialog wrapper class with DoNotAsl option for asking user to tweet.
   */
  private class TwitterDialogWrapper(
    project: Project,
    private val panel: TwitterDialogPanel,
    doNotAskOption: DoNotAskOption?
  ) : DialogWrapper(project) {

    init {
      title = message("twitter.dialog.title")
      setDoNotAskOption(doNotAskOption)
      setOKButtonText(message("twitter.dialog.ok.action"))
      setResizable(true)
      val preferredSize = panel.preferredSize
      setSize(preferredSize.getHeight().toInt(), preferredSize.getWidth().toInt())
      initValidation()
      init()
    }

    override fun createCenterPanel(): JComponent? = panel
    override fun doValidate(): ValidationInfo? = panel.doValidate()
  }
}
