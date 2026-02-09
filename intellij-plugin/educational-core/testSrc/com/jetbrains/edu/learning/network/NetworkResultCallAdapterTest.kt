package com.jetbrains.edu.learning.network

import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.testFramework.PlatformTestUtil
import com.jetbrains.edu.learning.*
import kotlinx.coroutines.runBlocking
import okhttp3.ConnectionPool
import okhttp3.mockwebserver.MockResponse
import org.junit.Test
import retrofit2.Call
import retrofit2.Response
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.http.GET
import java.net.HttpURLConnection.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import kotlin.test.assertIs

class NetworkResultCallAdapterTest : EduTestCase() {

  private lateinit var mockServer: MockWebServerHelper
  private lateinit var api: TestApi2

  override fun setUp() {
    super.setUp()
    mockServer = MockWebServerHelper(testRootDisposable)
    api = createApi()
  }

  override fun runInDispatchThread(): Boolean = false

  @Test
  fun `test ok sync`() {
    // given
    mockServer.addResponseHandler(testRootDisposable) { _, _ -> successfulResponse }

    // when
    val response = api.call().execute()

    // then
    assertIs<Response<*>>(response)
    assertEquals(response.code(), HTTP_OK)
    val responseBody = response.body()
    assertIs<Ok<Answer>>(responseBody)
    assertEquals(responseBody.value.value, MESSAGE)
  }

  @Test
  fun `test ok with empty body sync`() {
    // given
    mockServer.addResponseHandler(testRootDisposable) { _, _ -> MockResponse().setResponseCode(HTTP_NO_CONTENT) }

    // when
    val response = api.callNoContent().execute()

    // then
    assertIs<Response<*>>(response)
    assertEquals(response.code(), HTTP_OK)
    val responseBody = response.body()
    assertIs<Ok<*>>(responseBody)
    assertIs<Unit>(responseBody.value)
  }

  @Test
  fun `test ok async`() {
    // given
    mockServer.addResponseHandler(testRootDisposable) { _, _ -> successfulResponse }

    // when
    val response = runBlocking { api.callAsync() }

    // then
    assertIs<Ok<Answer>>(response)
    assertEquals(response.value.value, MESSAGE)
  }

  @Test
  fun `test ok with empty body async`() {
    // given
    mockServer.addResponseHandler(testRootDisposable) { _, _ -> MockResponse().setResponseCode(HTTP_NO_CONTENT) }

    // when
    val response = runBlocking { api.callAsyncNoContent() }

    // then
    assertIs<Ok<*>>(response)
    assertIs<Unit>(response.value)
  }


  @Test
  fun `test error sync`() {
    // given
    mockServer.addResponseHandler(testRootDisposable) { _, _ ->
      MockResponseFactory.notFound()
    }

    // when
    val response = api.call().execute()

    // then
    assertIs<Response<*>>(response)
    assertEquals(response.code(), HTTP_OK)

    val responseBody = response.body()
    assertIs<Err<*>>(responseBody)

    val error = responseBody.error
    assertIs<NetworkError.HttpError>(error)
    assertEquals(error.errorCode, HTTP_NOT_FOUND)
  }

  @Test
  fun `test error async`() {
    // given
    mockServer.addResponseHandler(testRootDisposable) { _, _ -> MockResponseFactory.notFound() }

    // when
    val response = runBlocking { api.callAsync() }

    // then
    assertIs<Err<*>>(response)
    val error = response.error
    assertIs<NetworkError.HttpError>(error)
    assertEquals(error.errorCode, HTTP_NOT_FOUND)
  }

  @Suppress("UsagesOfObsoleteApi")
  @Test
  fun `test progress cancellation sync`() {
    // given
    val requestStarted = CountDownLatch(1)
    val allowResponse = CountDownLatch(1)
    mockServer.addResponseHandler(testRootDisposable) { _, _ ->
      requestStarted.countDown()
      allowResponse.await()
      successfulResponse
    }

    // when
    val indicator = EmptyProgressIndicator()

    val future = CompletableFuture<Response<NetworkResult<Answer>>>()
    ProgressManager.getInstance().runProcessWithProgressAsynchronously(object : Task.Backgroundable(project, "Test title") {
      override fun run(indicator: ProgressIndicator) {
        val api = createApi()
        future.complete(api.call().execute())
      }

    }, indicator)

    // Imitate cancellation of the corresponding progress by a user when a long request is executing
    requestStarted.await()
    indicator.cancel()
    val response = PlatformTestUtil.waitForFuture(future, 500)
    allowResponse.countDown()

    // then
    assertIs<Response<*>>(response)
    assertEquals(response.code(), HTTP_OK)

    val responseBody = response.body()
    assertIs<Err<*>>(responseBody)

    val error = responseBody.error
    assertIs<NetworkError.Exception>(error)
  }

  private fun createApi(): TestApi2 =
    createRetrofitBuilder(mockServer.baseUrl, ConnectionPool())
      .addConverterFactory(JacksonConverterFactory.create())
      .build()
      .create(TestApi2::class.java)

  companion object {
    private const val MESSAGE = "Hello!"
    private val successfulResponse = MockResponseFactory.fromString("""{"value": "$MESSAGE"}""")
  }
}

private interface TestApi2 {
  @GET("/call")
  fun call(): Call<NetworkResult<Answer>>
  @GET("/callNoContent")
  fun callNoContent(): Call<NetworkResult<Unit>>
  @GET("/callSuspend")
  suspend fun callAsync(): NetworkResult<Answer>
  @GET("/callSuspendNoContent")
  suspend fun callAsyncNoContent(): NetworkResult<Unit>
}
