package com.jetbrains.edu.learning.marketplace.api

import com.intellij.openapi.util.Disposer
import com.intellij.util.application
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.actions.changeHost.ServiceHostManager.SelectedServiceHost
import com.jetbrains.edu.learning.courseFormat.JBAccountUserInfo
import com.jetbrains.edu.learning.marketplace.changeHost.LearningCenterServiceHost
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import kotlin.test.Test

class LearningCenterConnectorTest : EduTestCase() {
  private lateinit var marketplaceSubmissionsConnector: MarketplaceSubmissionsConnector

  private lateinit var marketplaceConnector: MarketplaceConnector

  private lateinit var learningCenterConnector: LearningCenterConnector

  private lateinit var helper: MockWebServerHelper

  override fun setUp() {
    super.setUp()
    helper = MockWebServerHelper(testRootDisposable)
    mockkObject(LearningCenterServiceHost.Companion)
    Disposer.register(testRootDisposable) {
      unmockkObject(LearningCenterServiceHost.Companion)
    }
    every { LearningCenterServiceHost.selectedHost } returns SelectedServiceHost(LearningCenterServiceHost.OTHER, helper.baseUrl)

    learningCenterConnector = mockService<LearningCenterConnector>(application)
    // The mock web server runs on localhost, so the real value of this property is always `true`
    every { learningCenterConnector.isLocalInstance } returns false

    marketplaceConnector = mockService<MarketplaceConnector>(application)
    marketplaceSubmissionsConnector = mockService<MarketplaceSubmissionsConnector>(application)
    coEvery { marketplaceSubmissionsConnector.getUserJWT() } returns Ok(TOKEN)
  }

  @Test
  fun `test certificate is ready`() = runTest {
    // given
    val responseBody = """
      {
        "type": "ready",
        "certificateId": "certificate-id",
        "isFirstClientRequest": true
      }
    """.trimIndent()
    configureResponse(MockResponseFactory.fromString(responseBody))

    // when
    val response = LearningCenterConnector.getInstance().tryIssueCertificate(COMPLETED_PERCENT, COURSE_ID)

    // then
    assertEquals(Ok(CourseCertificateIssueResponse.CertificateReady("certificate-id", true)), response)
  }

  @Test
  fun `test certificate is pending`() = runTest {
    // given
    val responseBody = """
      {
        "type": "pending",
        "certificateId": "certificate-id",
        "isFirstClientRequest": false
      }
    """.trimIndent()
    configureResponse(MockResponseFactory.fromString(responseBody))

    // when
    val response = LearningCenterConnector.getInstance().tryIssueCertificate(COMPLETED_PERCENT, COURSE_ID)

    // then
    assertEquals(Ok(CourseCertificateIssueResponse.CertificatePending("certificate-id", false)), response)
  }

  @Test
  fun `test certificate is not ready`() = runTest {
    // given
    val responseBody = """
      {
        "type": "not_ready",
        "requiredPercent": 80
      }
    """.trimIndent()
    configureResponse(MockResponseFactory.fromString(responseBody))

    // when
    val response = LearningCenterConnector.getInstance().tryIssueCertificate(COMPLETED_PERCENT, COURSE_ID)

    // then
    assertEquals(Ok(CourseCertificateIssueResponse.NotReady(80)), response)
  }

  @Test
  fun `test certificate issuing is unsupported`() = runTest {
    // given
    val responseBody = """
      {
        "type": "unsupported"
      }
    """.trimIndent()
    configureResponse(MockResponseFactory.fromString(responseBody))

    // when
    val response = LearningCenterConnector.getInstance().tryIssueCertificate(COMPLETED_PERCENT, COURSE_ID)

    // then
    assertEquals(Ok(CourseCertificateIssueResponse.Unsupported), response)
  }

  @Test
  fun `test unknown properties in certificate issuing response are ignored`() = runTest {
    // given
    val responseBody = """
      {
        "type": "ready",
        "certificateId": "certificate-id",
        "isFirstClientRequest": true,
        "issuedAt": "2026-09-01"
      }
    """.trimIndent()
    configureResponse(MockResponseFactory.fromString(responseBody))

    // when
    val response = LearningCenterConnector.getInstance().tryIssueCertificate(COMPLETED_PERCENT, COURSE_ID)

    // then
    assertEquals(Ok(CourseCertificateIssueResponse.CertificateReady("certificate-id", true)), response)
  }

  @Test
  fun `test unknown certificate issuing response type is treated as unsupported`() = runTest {
    // given
    val responseBody = """
      {
        "type": "revoked",
        "certificateId": "certificate-id"
      }
    """.trimIndent()
    configureResponse(MockResponseFactory.fromString(responseBody))

    // when
    val response = LearningCenterConnector.getInstance().tryIssueCertificate(COMPLETED_PERCENT, COURSE_ID)

    // then
    assertEquals(Ok(CourseCertificateIssueResponse.Unsupported), response)
  }

  @Test
  fun `test certification support response without type is treated as unsupported`() = runTest {
    // given
    val responseBody = "{}"
    configureResponse(MockResponseFactory.fromString(responseBody))

    // when
    val response = LearningCenterConnector.getInstance().courseCertificationSupport(COURSE_ID)

    // then
    assertEquals(Ok(CourseCertificationSupportResponse.Unsupported), response)
  }

  @Test
  fun `test failed response during certificate issuing`() = runTest {
    // given
    configureResponse(MockResponseFactory.notFound())

    // when
    val response = LearningCenterConnector.getInstance().tryIssueCertificate(COMPLETED_PERCENT, COURSE_ID)

    // then
    assertTrue("Expected an error, but got $response", response is Err)
  }

  @Test
  fun `test certificate is not issued without JB account access token`() = runTest {
    // given
    coEvery { marketplaceSubmissionsConnector.getUserJWT() } returns Err("Could not issue JWT")
    configureResponse(MockResponseFactory.ok())

    // when
    val response = LearningCenterConnector.getInstance().tryIssueCertificate(COMPLETED_PERCENT, COURSE_ID)

    // then
    assertTrue("Expected an error, but got $response", response is Err)
  }

  @Test
  fun `test certificate is issued with dev authorization for local learning center`() = runTest {
    // given
    every { learningCenterConnector.isLocalInstance } returns true
    every { marketplaceConnector.getCurrentUserInfo() } returns JBAccountUserInfo("Test User").apply { email = EMAIL }

    val responseBody = """
      {
        "type": "ready",
        "certificateId": "certificate-id",
        "isFirstClientRequest": false
      }
    """.trimIndent()
    configureResponse(MockResponseFactory.fromString(responseBody), expectedAuth = DEV_AUTH_HEADER to EMAIL)

    // when
    val response = LearningCenterConnector.getInstance().tryIssueCertificate(COMPLETED_PERCENT, COURSE_ID)

    // then
    assertEquals(Ok(CourseCertificateIssueResponse.CertificateReady("certificate-id", false)), response)
    coVerify(exactly = 0) { marketplaceSubmissionsConnector.getUserJWT() }
  }

  @Test
  fun `test certificate is not issued for local learning center without JB account email`() = runTest {
    // given
    every { learningCenterConnector.isLocalInstance } returns true
    every { marketplaceConnector.getCurrentUserInfo() } returns null
    configureResponse(MockResponseFactory.ok())

    // when
    val response = LearningCenterConnector.getInstance().tryIssueCertificate(COMPLETED_PERCENT, COURSE_ID)

    // then
    assertTrue("Expected an error, but got $response", response is Err)
  }

  @Test
  fun `test certification support is checked without authorization for local learning center`() = runTest {
    // given
    every { learningCenterConnector.isLocalInstance } returns true

    val responseBody = """
      {
        "type": "supported",
        "requiredPercent": 90
      }
    """.trimIndent()
    configureResponse(MockResponseFactory.fromString(responseBody))

    // when
    val response = LearningCenterConnector.getInstance().courseCertificationSupport(COURSE_ID)

    // then
    assertEquals(Ok(CourseCertificationSupportResponse.Supported(90)), response)
  }

  @Test
  fun `test course supports certification`() = runTest {
    // given
    val responseBody = """
      {
        "type": "supported",
        "requiredPercent": 90
      }
    """.trimIndent()
    configureResponse(MockResponseFactory.fromString(responseBody))

    // when
    val response = LearningCenterConnector.getInstance().courseCertificationSupport(COURSE_ID)

    // then
    assertEquals(Ok(CourseCertificationSupportResponse.Supported(90)), response)
  }

  @Test
  fun `test course does not support certification`() = runTest {
    // given
    val responseBody = """
      {
        "type": "unsupported"
      }
    """.trimIndent()
    configureResponse(MockResponseFactory.fromString(responseBody))

    // when
    val response = LearningCenterConnector.getInstance().courseCertificationSupport(COURSE_ID)

    // then
    assertEquals(Ok(CourseCertificationSupportResponse.Unsupported), response)
  }

  @Test
  fun `test failed response during certification support check`() = runTest {
    // given
    configureResponse(MockResponseFactory.notFound())

    // when
    val response = LearningCenterConnector.getInstance().courseCertificationSupport(COURSE_ID)

    // then
    assertTrue("Expected an error, but got $response", response is Err)
  }

  /**
   * Replies with [response] to a valid request, and with `400 Bad Request` otherwise.
   * A request to the endpoint requiring an authorized user is valid only if it contains the [expectedAuth] header
   */
  private fun configureResponse(response: MockResponse, expectedAuth: Pair<String, String> = AUTHORIZATION_HEADER to "Bearer $TOKEN") {
    helper.addResponseHandler(testRootDisposable) { request, _ ->
      if (!request.hasParams("courseId" to COURSE_ID.toString())) {
        return@addResponseHandler MockResponseFactory.badRequest()
      }
      val (expectedAuthHeader, expectedAuthValue) = expectedAuth

      when (request.pathWithoutPrams) {
        // the endpoint requires an authorized user and the current progress
        ISSUE_PATH -> {
          val validRequest = request.headers[expectedAuthHeader] == expectedAuthValue &&
                             request.hasParams("completedPercent" to COMPLETED_PERCENT.toString())
          if (validRequest) response else MockResponseFactory.badRequest()
        }
        // the endpoint is expected to be called without authorization
        HAS_CERTIFICATE_PATH -> {
          val authorized = request.headers[AUTHORIZATION_HEADER] != null || request.headers[DEV_AUTH_HEADER] != null
          if (authorized) MockResponseFactory.badRequest() else response
        }

        else -> MockResponseFactory.notFound()
      }
    }
  }

  companion object {
    private const val TOKEN = "jba-access-token"
    private const val EMAIL = "test@jetbrains.com"
    private const val COURSE_ID = 12345
    private const val COMPLETED_PERCENT = 75
    private const val ISSUE_PATH = "/api/courses/progress/certificates/issue"
    private const val HAS_CERTIFICATE_PATH = "/api/courses/progress/certificates/has-certificate"
    private const val AUTHORIZATION_HEADER = "Authorization"
    private const val DEV_AUTH_HEADER = "X-Dev-Auth"
  }
}
