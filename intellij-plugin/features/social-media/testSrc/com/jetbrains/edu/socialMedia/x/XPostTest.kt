package com.jetbrains.edu.socialMedia.x

import com.intellij.util.application
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.mockService
import com.jetbrains.edu.learning.runAndWaitForNotification
import com.jetbrains.edu.socialMedia.messages.EduSocialMediaBundle
import com.jetbrains.edu.socialMedia.x.api.TweetData
import com.jetbrains.edu.socialMedia.x.api.TweetResponse
import io.mockk.every
import io.mockk.verify
import org.junit.Test
import java.nio.file.Paths

class XPostTest : EduTestCase() {

  @Test
  fun `test successful post when logged in`() {
    // given
    val mockConnector = mockService<XConnector>(application)
    mockConnector.account = XAccount.Factory.create()
    every { mockConnector.tweet(any(), any()) } returns RESPONSE

    // when
    val notification = runAndWaitForNotification(project) {
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
    val notification = runAndWaitForNotification(project) {
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
    val notification = runAndWaitForNotification(project) {
      XUtils.doPost(project, TWEET_TEXT, null)
    }

    // then
    verify(exactly = 0) { mockConnector.doAuthorize(any()) }
    verify(exactly = 1) { mockConnector.tweet(TWEET_TEXT, null) }
    assertEquals(EduSocialMediaBundle.message("social.media.error.failed.to.post.notification"), notification.content)
  }

  @Test
  fun `test error during posting`() {
    // given
    val mockConnector = mockService<XConnector>(application)
    mockConnector.account = XAccount.Factory.create()
    every { mockConnector.tweet(any(), any()) } returns null

    // when
    val notification = runAndWaitForNotification(project) {
      XUtils.doPost(project, TWEET_TEXT, null)
    }

    // then
    verify(exactly = 0) { mockConnector.doAuthorize(any()) }
    verify(exactly = 1) { mockConnector.tweet(TWEET_TEXT, null) }
    assertEquals(EduSocialMediaBundle.message("social.media.error.failed.to.post.notification"), notification.content)
  }

  companion object {
    private const val TWEET_TEXT = "Hello!"
    private val RESPONSE = TweetResponse(TweetData("123", TWEET_TEXT))
    private val PATH_TO_MEDIA = Paths.get("/path/to/gif_file.gif")
  }
}
