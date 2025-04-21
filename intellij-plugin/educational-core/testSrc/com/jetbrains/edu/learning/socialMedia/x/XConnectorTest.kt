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
import com.jetbrains.edu.rules.WithRegistryValue
import com.jetbrains.rd.util.ConcurrentHashMap
import org.junit.Test
import java.net.HttpURLConnection.*
import java.nio.file.Paths
import java.util.Collections
import kotlin.collections.ArrayDeque
import kotlin.test.assertContains
import kotlin.test.assertNotNull as kAssertNotNull

@WithRegistryValue("edu.socialMedia.x.oauth2", "true")
class XConnectorTest : EduTestCase() {

  private lateinit var helper: MockWebServerHelper

  private val requestBodies: MutableMap<String, MutableList<String>> = ConcurrentHashMap()

  override fun setUp() {
    super.setUp()
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

  @Test
  fun `test tweet posting with media`() {
    // given
    val uploadMediaAnswers = ArrayDeque(MEDIA_RESPONSES)
    helper.addResponseHandlerWithRequestBodyRecording { request, path ->
      when (path) {
        "/2/tweets" -> MockResponseFactory.fromString(TWEET_RESPONSE)
        "/2/media/upload" -> {
          val (code, body) = uploadMediaAnswers.removeFirst()
          MockResponseFactory.fromString(body, code)
        }
        "/2/media/upload?media_id=$MEDIA_ID" -> MockResponseFactory.fromString(MEDIA_STATUS_RESPONSE)
        else -> null
      }
    }
    val mediaPath = Paths.get(testDataPath).resolve("socialMedia/mock_media.gif").toAbsolutePath()

    // when
    val response = XConnector.getInstance().tweet(TWEET_TEXT, mediaPath)

    // then
    val uploadMediaRequests = kAssertNotNull(requestBodies["/2/media/upload"])

    // check that we pass INIT command
    assertContains(uploadMediaRequests[0], "INIT")
    // check that we pass APPEND command with the correct media id
    assertContains(uploadMediaRequests[1], "APPEND")
    assertContains(uploadMediaRequests[1], MEDIA_ID)
    // check that we pass FINALIZE command with the correct media id
    assertContains(uploadMediaRequests[2], "FINALIZE")
    assertContains(uploadMediaRequests[2], MEDIA_ID)

    // check that the corresponding request was called
    kAssertNotNull(requestBodies["/2/media/upload?media_id=$MEDIA_ID"]?.singleOrNull())

    val requestBody = kAssertNotNull(requestBodies["/2/tweets"]?.singleOrNull())
    val tweet = XConnector.getInstance().objectMapper.readValue<Tweet>(requestBody)
    assertEquals(Tweet(TWEET_TEXT, Media(listOf(MEDIA_ID))), tweet)

    assertEquals(TweetResponse(TweetData(TWEET_ID, TWEET_TEXT)), response)
  }

  @Test
  fun `test tweet posting with media with error`() {
    // given
    helper.addResponseHandlerWithRequestBodyRecording { request, path ->
      when (path) {
        "/2/tweets" -> MockResponseFactory.fromString(TWEET_RESPONSE)
        "/2/media/upload" -> MockResponseFactory.badRequest()
        else -> null
      }
    }
    val mediaPath = Paths.get(testDataPath).resolve("socialMedia/mock_media.gif").toAbsolutePath()

    // when
    val response = XConnector.getInstance().tweet(TWEET_TEXT, mediaPath)

    // then
    kAssertNotNull(requestBodies["/2/media/upload"]?.singleOrNull())
    assertNull(requestBodies["/2/tweets"])
    assertNull(null, response)
  }

  private fun mockXAccount() {
    val account = XAccount.Factory.create()
    val tokenInfo = TokenInfo().apply {
      accessToken = ACCESS_TOKEN
      refreshToken = REFRESH_TOKEN
      expiresIn = account.tokenExpiresIn
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
    private const val MEDIA_ID = "1912475018862166016"

    //language=json
    private const val TWEET_RESPONSE = """{"data":{"edit_history_tweet_ids":["$TWEET_ID"],"id":"$TWEET_ID","text":"Hello!"}}"""

    //language=json
    private const val MEDIA_STATUS_RESPONSE = """{"data":{"id":"$MEDIA_ID","media_key":"16_1912475018862166016","expires_after_secs":86398,"processing_info":{"state":"succeeded","progress_percent":100},"size":708679}}"""

    //language=json
    private val MEDIA_RESPONSES = listOf(
      HTTP_ACCEPTED to """{"data":{"id":"$MEDIA_ID","media_key":"16_1912475018862166016","expires_after_secs":86400}}""",
      HTTP_NO_CONTENT to "",
      HTTP_OK to """{"data":{"id":"$MEDIA_ID","media_key":"16_1912475018862166016","expires_after_secs":86400,"processing_info":{"state":"pending","check_after_secs":1},"size":708679}}""",
    )
  }
}
