package com.jetbrains.edu.learning.network

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.MockWebServerHelper
import com.jetbrains.edu.learning.Ok
import kotlinx.coroutines.runBlocking
import okhttp3.ConnectionPool
import okhttp3.mockwebserver.MockResponse
import org.junit.Test
import retrofit2.Response
import retrofit2.converter.jackson.JacksonConverterFactory
import java.net.HttpURLConnection.HTTP_BAD_GATEWAY
import java.net.HttpURLConnection.HTTP_INTERNAL_ERROR
import java.net.HttpURLConnection.HTTP_UNAVAILABLE
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.milliseconds

class RetryPolicyTest : EduTestCase() {

  private lateinit var mockServer: MockWebServerHelper

  override fun setUp() {
    super.setUp()
    mockServer = MockWebServerHelper(testRootDisposable)
  }

  @Test
  fun `test no retry policy`() {
    // given
    val counter = AtomicInteger()
    mockServer.addResponseHandler(testRootDisposable) { _, _ ->
      val attempt = counter.incrementAndGet()
      when (attempt) {
        1 -> MockResponse().setResponseCode(HTTP_INTERNAL_ERROR)
        else -> successfulResponse
      }
    }

    // when
    val api = createApi(null)
    val response = api.call().executeCall()

    // then
    assertIs<Ok<Response<Answer>>>(response)
    assertEquals(response.value.code(), HTTP_INTERNAL_ERROR)

    assertEquals(1, counter.get())
  }

  @Test
  fun `test retry policy`() {
    // given
    val counter = AtomicInteger()
    mockServer.addResponseHandler(testRootDisposable) { _, _ ->
      val attempt = counter.incrementAndGet()
      when (attempt) {
        1 -> MockResponse().setResponseCode(HTTP_UNAVAILABLE)
        else -> successfulResponse
      }
    }

    // when
    val api = createApi()
    val response = api.call().executeCall()

    // then
    assertIs<Ok<Response<Answer>>>(response)
    assertEquals(Answer(MESSAGE), response.value.body())

    assertEquals(2, counter.get())
  }

  @Test
  fun `test retry policy with suspend call`() {
    // given
    val counter = AtomicInteger()
    mockServer.addResponseHandler(testRootDisposable) { _, _ ->
      val attempt = counter.incrementAndGet()
      when (attempt) {
        1 -> MockResponse().setResponseCode(HTTP_UNAVAILABLE)
        else -> successfulResponse
      }
    }

    // when
    val api = createApi()
    val response = runBlocking { api.callAsync() }

    // then
    assertIs<Answer>(response)
    assertEquals(Answer(MESSAGE), response)

    assertEquals(2, counter.get())
  }

  @Test
  fun `test max retry attempts`() {
    // given
    val counter = AtomicInteger()
    mockServer.addResponseHandler(testRootDisposable) { _, _ ->
      counter.incrementAndGet()
      MockResponse().setResponseCode(HTTP_BAD_GATEWAY)
    }

    // when
    val api = createApi()
    val response = api.call().executeCall()

    // then
    assertIs<Ok<Response<Answer>>>(response)
    assertEquals(response.value.code(), HTTP_BAD_GATEWAY)

    assertEquals(3, counter.get())
  }

  private fun createApi(retryPolicy: RetryPolicy? = defaultRetryPolicy): TestApi =
    createRetrofitBuilder(mockServer.baseUrl, ConnectionPool(), retryPolicy = retryPolicy)
      .addConverterFactory(JacksonConverterFactory.create())
      .build()
      .create(TestApi::class.java)

  companion object {
    private const val MESSAGE = "Hello!"

    private val successfulResponse = MockResponseFactory.fromString("""{"value": "$MESSAGE"}""")
    private val defaultRetryPolicy = RetryPolicy(retryAttempts = 2, initialDelay = 10.milliseconds)
  }
}
