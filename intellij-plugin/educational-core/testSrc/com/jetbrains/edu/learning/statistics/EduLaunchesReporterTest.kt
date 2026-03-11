package com.jetbrains.edu.learning.statistics

import com.intellij.ide.util.PropertiesComponent
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.MockResponseFactory.internalError
import com.jetbrains.edu.learning.MockWebServerHelper
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.EduFormatNames
import com.jetbrains.edu.learning.statistics.EduLaunchesReporter.Companion.LAST_UPDATE
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.greaterThan
import org.junit.Test
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@OptIn(ExperimentalCoroutinesApi::class)
class EduLaunchesReporterTest : EduTestCase() {

  private lateinit var mockServer: MockWebServerHelper

  override fun setUp() {
    super.setUp()
    mockServer = MockWebServerHelper(testRootDisposable)
  }

  override fun tearDown() {
    try {
      PropertiesComponent.getInstance().unsetValue(LAST_UPDATE)
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }

  override fun runInDispatchThread(): Boolean = false

  @Test
  fun `test successful sending stats`() = runTest {
    // given
    val requestCount = AtomicInteger()
    val request = AtomicReference<RecordedRequest>(null)
    mockServer.addResponseHandler(testRootDisposable) { r, _ ->
      request.set(r)
      requestCount.incrementAndGet()
      xmlResponse()
    }
    val reporter = createReporter()

    // when
    reporter.sendStats(course)
    advanceUntilIdle()

    // then
    assertEquals(1, requestCount.get())
    request.get().checkRequestData()

    val timestamp = PropertiesComponent.getInstance().getLong(LAST_UPDATE, 0)
    assertThat("$LAST_UPDATE timestamp should not be empty or negative", timestamp, greaterThan(0))
  }

  @Test
  fun `test sequential statistics requests`() = runTest {
    // given
    val requestCount = AtomicInteger()
    val request = AtomicReference<RecordedRequest>(null)
    mockServer.addResponseHandler(testRootDisposable) { r, _ ->
      request.set(r)
      requestCount.incrementAndGet()
      xmlResponse()
    }
    val reporter = createReporter()

    // when
    reporter.sendStats(course)
    advanceUntilIdle()
    val timestamp1 = PropertiesComponent.getInstance().getLong(LAST_UPDATE, 0)

    reporter.sendStats(course) // emulate sequential statistics requests
    advanceUntilIdle()
    val timestamp2 = PropertiesComponent.getInstance().getLong(LAST_UPDATE, 0)

    // then
    assertEquals(1, requestCount.get())
    request.get().checkRequestData()
    assertThat("$LAST_UPDATE timestamp should not be empty or negative", timestamp1, greaterThan(0))
    assertEquals("$LAST_UPDATE timestamp shouldn't change", timestamp1, timestamp2)
  }

  @Test
  fun `test sending stats after long delay`() = runTest {
    // given
    val requestCount = AtomicInteger()
    val request = AtomicReference<RecordedRequest>(null)
    mockServer.addResponseHandler(testRootDisposable) { r, _ ->
      request.set(r)
      requestCount.incrementAndGet()
      xmlResponse()
    }
    val reporter = createReporter()
    // emulate delay which is long enough to send a new statistics request
    val previousTimestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2)
    PropertiesComponent.getInstance().setValue(LAST_UPDATE, previousTimestamp.toString())

    // when
    reporter.sendStats(course)
    advanceUntilIdle()

    // then
    assertEquals(1, requestCount.get())
    request.get().checkRequestData()

    val timestamp = PropertiesComponent.getInstance().getLong(LAST_UPDATE, 0)
    assertThat("$LAST_UPDATE timestamp should not be empty or negative", timestamp, greaterThan(0))
    assertThat("$LAST_UPDATE timestamp should be updated", timestamp, greaterThan(previousTimestamp))
  }

  @Test
  fun `test retry stops after success`() = runTest {
    // given
    val requestCount = AtomicInteger()
    val request = AtomicReference<RecordedRequest>(null)
    val failCount = 3
    mockServer.addResponseHandler(testRootDisposable) { r, _ ->
      val attempt = requestCount.incrementAndGet()
      if (attempt <= failCount) {
        internalError()
      }
      else {
        request.set(r)
        xmlResponse()
      }
    }
    val reporter = createReporter()

    // when
    reporter.sendStats(course)
    advanceUntilIdle()

    // then
    assertEquals(failCount + 1, requestCount.get())
    request.get().checkRequestData()
    val timestamp = PropertiesComponent.getInstance().getLong(LAST_UPDATE, 0)
    assertThat("$LAST_UPDATE timestamp should not be empty or negative", timestamp, greaterThan(0))
  }

  @Test
  fun `test retry stops after max attempts`() = runTest {
    // given
    val requestCount = AtomicInteger()
    mockServer.addResponseHandler(testRootDisposable) { _, _ ->
      requestCount.incrementAndGet()
      internalError()
    }
    val reporter = createReporter()

    // when
    reporter.sendStats(course)
    advanceUntilIdle()

    // then
    assertEquals(EduLaunchesReporter.MAX_RETRY_ATTEMPTS + 1, requestCount.get())
    assertEquals("$LAST_UPDATE timestamp should be empty", 0, PropertiesComponent.getInstance().getLong(LAST_UPDATE, 0))
  }

  @Test
  fun `test parallel calls send single request`() = runTest {
    val requestCount = AtomicInteger()
    val requests = Collections.synchronizedList<RecordedRequest>(mutableListOf())
    mockServer.addResponseHandler(testRootDisposable) { r, _ ->
      requests.add(r)
      val attempt = requestCount.incrementAndGet()
      if (attempt > 1) {
        xmlResponse()
      }
      else {
        internalError()
      }
    }

    // the second course object is needed to simplify distinction of stasistics requests
    val secondCourse = EduCourse().apply {
      id = 1234
      isMarketplace = true
    }

    val reporter = createReporter()

    // when
    reporter.sendStats(course)
    runCurrent()
    reporter.sendStats(secondCourse)
    advanceUntilIdle()

    // then
    assertEquals(2, requestCount.get())
    // verify that we don't have unexpected requests with id of the second course
    requests.forEach { it.checkRequestData() }
    val timestamp = PropertiesComponent.getInstance().getLong(LAST_UPDATE, 0)
    assertThat("$LAST_UPDATE timestamp should not be empty or negative", timestamp, greaterThan(0))
  }

  private fun TestScope.createReporter(): EduLaunchesReporter {
    val testDispatcher = StandardTestDispatcher(testScheduler)
    return EduLaunchesReporter(this, mockServerBaseUrl(), sendStatsInTests = true, ioDispatcher = testDispatcher)
  }

  private fun mockServerBaseUrl(): String = mockServer.baseUrl + "plugins/list"

  private fun xmlResponse(): MockResponse = MockResponseFactory
    .ok()
    .setHeader("Content-Type", "application/xml")
    .setBody("<plugin-repository/>")

  private fun RecordedRequest.checkRequestData() {
    val requestData = parseData()
    assertEquals(COURSE_ID.toString(), requestData.courseId)
    assertEquals(EduFormatNames.MARKETPLACE, requestData.projectType)
    assertEquals("student", requestData.role)
  }

  private fun RecordedRequest.parseData(): RequestData {
    val url = requestUrl ?: error("Request url should not be null")
    return RequestData(
      pluginId = url.parameter("pluginId"),
      build = url.parameter("build"),
      pluginVersion = url.parameter("pluginVersion"),
      os = url.parameter("os"),
      uuid = url.parameter("uuid"),
      projectType = url.parameter("projectType"),
      role = url.parameter("role"),
      courseId = url.parameter("courseId"),  
    )
  }
  
  private fun HttpUrl.parameter(name: String): String {
    return queryParameter(name) ?: error("`$name` parameter should not be null")
  }

  companion object {
    private const val COURSE_ID = 123

    private val course = EduCourse().apply {
      id = COURSE_ID
      isMarketplace = true
    }
  }
  
  private data class RequestData(
    val pluginId: String,
    val build: String,
    val pluginVersion: String,
    val os: String,
    val uuid: String,
    val projectType: String,
    val role: String,
    val courseId: String
  )
}
