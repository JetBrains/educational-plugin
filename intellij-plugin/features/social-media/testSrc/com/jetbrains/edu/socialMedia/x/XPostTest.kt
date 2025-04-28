package com.jetbrains.edu.socialMedia.x

import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.util.application
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.mockService
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.edu.rules.WithRegistryValue
import com.jetbrains.edu.socialMedia.messages.EduSocialMediaBundle
import com.jetbrains.edu.socialMedia.x.api.TweetData
import com.jetbrains.edu.socialMedia.x.api.TweetResponse
import io.mockk.every
import io.mockk.verify
import org.junit.Test
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicReference

@WithRegistryValue("edu.socialMedia.x.oauth2", "true")
class XPostTest : EduTestCase() {

  @Test
  fun `test successful post when logged in`() {
    // given
    val mockConnector = mockService<XConnector>(application)
    mockConnector.account = XAccount.Factory.create()
    every { mockConnector.tweet(any(), any()) } returns RESPONSE

    // when
    val notification = runAndWaitForNotification {
      XUtils.doPost(project, TWEET_TEXT, null)
    }

    // then
    verify(exactly = 0) { mockConnector.doAuthorize(any()) }
    verify(exactly = 1) { mockConnector.tweet(TWEET_TEXT, null) }
    assertEquals(EduSocialMediaBundle.message("x.tweet.posted"), notification.content)
  }

  @Test
  fun `test successful post when not logged in`() {
    // given
    val mockConnector = mockService<XConnector>(application)
    every { mockConnector.tweet(any(), any()) } returns RESPONSE

    val mockBrowser = mockService<EduBrowser>(application)
    every { mockBrowser.browse(any<String>()) } answers {
      // emulates the successful OAuth2.0 flow
      mockConnector.account = XAccount.Factory.create()
      mockConnector.notifyUserLoggedIn()
    }

    // when
    val notification = runAndWaitForNotification {
      XUtils.doPost(project, TWEET_TEXT, PATH_TO_MEDIA)
    }

    // then
    verify(exactly = 1) { mockConnector.doAuthorize(any()) }
    verify(exactly = 1) { mockConnector.tweet(TWEET_TEXT, PATH_TO_MEDIA) }
    assertEquals(EduSocialMediaBundle.message("x.tweet.posted"), notification.content)
  }

  @Test
  fun `test exception during posting`() {
    // given
    val mockConnector = mockService<XConnector>(application)
    mockConnector.account = XAccount.Factory.create()
    every { mockConnector.tweet(any(), any()) } throws RuntimeException("Error posting tweet")

    // when
    val notification = runAndWaitForNotification {
      XUtils.doPost(project, TWEET_TEXT, null)
    }

    // then
    verify(exactly = 0) { mockConnector.doAuthorize(any()) }
    verify(exactly = 1) { mockConnector.tweet(TWEET_TEXT, null) }
    assertEquals(EduSocialMediaBundle.message("linkedin.error.failed.to.post"), notification.content)
  }

  @Test
  fun `test error during posting`() {
    // given
    val mockConnector = mockService<XConnector>(application)
    mockConnector.account = XAccount.Factory.create()
    every { mockConnector.tweet(any(), any()) } returns null

    // when
    val notification = runAndWaitForNotification {
      XUtils.doPost(project, TWEET_TEXT, null)
    }

    // then
    verify(exactly = 0) { mockConnector.doAuthorize(any()) }
    verify(exactly = 1) { mockConnector.tweet(TWEET_TEXT, null) }
    assertEquals(EduSocialMediaBundle.message("linkedin.error.failed.to.post"), notification.content)
  }

  private fun runAndWaitForNotification(action: () -> Unit): Notification {
    val shownNotification = AtomicReference<Notification>()
    val connection = project.messageBus.connect(testRootDisposable)
    connection.subscribe(Notifications.TOPIC, object : Notifications {
      override fun notify(notification: Notification) {
        if (notification.groupId == EduNotificationManager.JETBRAINS_ACADEMY_GROUP_ID) {
          shownNotification.set(notification)
          connection.disconnect()
        }
      }
    })

    action()

    PlatformTestUtil.waitWhileBusy { shownNotification.get() == null }

    return shownNotification.get()
  }

  companion object {
    private const val TWEET_TEXT = "Hello!"
    private val RESPONSE = TweetResponse(TweetData("123", TWEET_TEXT))
    private val PATH_TO_MEDIA = Paths.get("/path/to/gif_file.gif")
  }
}
