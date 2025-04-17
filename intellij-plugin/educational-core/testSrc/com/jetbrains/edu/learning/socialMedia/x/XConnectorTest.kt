package com.jetbrains.edu.learning.socialMedia.x

import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.testFramework.replaceService
import com.intellij.util.application
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.MockWebServerHelper
import com.jetbrains.edu.learning.ResponseHandler
import com.jetbrains.edu.learning.authUtils.TokenInfo
import com.jetbrains.edu.learning.socialMedia.x.api.Media
import com.jetbrains.edu.learning.socialMedia.x.api.Tweet
import com.jetbrains.edu.learning.socialMedia.x.api.TweetData
import com.jetbrains.edu.learning.socialMedia.x.api.TweetResponse
import com.jetbrains.rd.util.ConcurrentHashMap
import org.junit.Test
import java.util.*
import kotlin.test.assertNotNull as kAssertNotNull

class XConnectorTest : EduTestCase() {

  private lateinit var helper: MockWebServerHelper

  private val requestBodies: MutableMap<String, MutableList<String>> = ConcurrentHashMap()

  override fun setUp() {
    super.setUp()
    enableAuth2ForX(testRootDisposable)
    inMemoryPasswordSafe(testRootDisposable)
    mockXAccount()
    helper = MockWebServerHelper(testRootDisposable)
    application.replaceService(XConnector::class.java, XConnector(helper.baseUrl, helper.baseUrl, "12345"), testRootDisposable)
  }

  @Test
  fun `test tweet posting without media`() {
    // given
    helper.addResponseHandlerWithRequestBodyRecording { request, path ->
      when (path) {
        "/2/tweets" -> MockResponseFactory.fromString(TWEET_RESPONSE)
        else -> null
      }
    }

    // when
    val response = XConnector.getInstance().tweet(TWEET_TEXT, null)

    // then
    val requestBody = kAssertNotNull(requestBodies["/2/tweets"]?.singleOrNull())
    val tweet = XConnector.getInstance().objectMapper.readValue<Tweet>(requestBody)
    assertEquals(Tweet(TWEET_TEXT, Media(listOf())), tweet)

    assertEquals(TweetResponse(TweetData(TWEET_ID, TWEET_TEXT)), response)
  }

  @Test
  fun `test tweet posting without media with error`() {
    // given
    helper.addResponseHandlerWithRequestBodyRecording { request, path ->
      when (path) {
        "/2/tweets" -> MockResponseFactory.badRequest()
        else -> null
      }
    }

    // when
    val response = XConnector.getInstance().tweet(TWEET_TEXT, null)

    // then
    assertNull(response)
  }

  private fun mockXAccount() {
    val account = XAccount.Factory.create()
    val tokenInfo = TokenInfo().apply {
      this.accessToken = ACCESS_TOKEN
      this.refreshToken = REFRESH_TOKEN
      this.expiresIn = account.tokenExpiresIn
    }

    account.saveTokens(tokenInfo)
    // it's ok not to drop mock account at the end of the test.
    // `XSettings` can do it itself
    XSettings.getInstance().account = account
  }

  private fun MockWebServerHelper.addResponseHandlerWithRequestBodyRecording(handler: ResponseHandler) {
    addResponseHandler(testRootDisposable) { request, path ->
      val requests = requestBodies.getOrPut(path) {
        Collections.synchronizedList(mutableListOf())
      }
      requests += request.body.readUtf8()

      // Verify that we pass the proper access token here
      if (request.getHeader("Authorization") != "Bearer $ACCESS_TOKEN") {
        return@addResponseHandler MockResponseFactory.badRequest()
      }

      handler(request, path)
    }
  }

  companion object {
    private const val TWEET_TEXT = "Hello!"

    private const val ACCESS_TOKEN = "12345"
    private const val REFRESH_TOKEN = "67890"

    private const val TWEET_ID = "1912475036826448076"

    //language=json
    private const val TWEET_RESPONSE = """{"data":{"edit_history_tweet_ids":["$TWEET_ID"],"id":"$TWEET_ID","text":"Hello!"}}"""
  }
}
