package com.jetbrains.edu.learning.marketplace.license

import com.intellij.util.application
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.marketplace.license.api.LicenseConnector
import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import org.intellij.lang.annotations.Language
import kotlin.test.Test

class LicenseConnectorTest : EduTestCase() {
  private lateinit var mockServer: MockWebServerHelper

  private val link: String
    get() = mockServer.baseUrl + API_PATH

  override fun setUp() {
    super.setUp()
    val marketplaceSubmissionsConnector = mockService<MarketplaceSubmissionsConnector>(application)
    coEvery { marketplaceSubmissionsConnector.getLicenseJWT() } returns Ok(JWT)
    mockServer = MockWebServerHelper(testRootDisposable)
  }

  @Test
  fun `test successful license check response`() = runTest {
    // given
    @Language("JSON")
    val responseBody = "{}"
    configureResponse(MockResponseFactory.fromString(responseBody))

    // when
    val licenseState = LicenseConnector.getInstance().checkLicense(link)

    // then
    assertEquals(LicenseState.VALID, licenseState)
  }

  @Test
  fun `test expired license check response`() = runTest {
    // given
    @Language("JSON")
    val responseBody = """
      {
        "trackType": "AWS"
      }
    """.trimIndent()
    configureResponse(MockResponseFactory.fromString(responseBody))

    // when
    val response = LicenseConnector.getInstance().checkLicense(link)

    // then
    assertEquals(LicenseState.EXPIRED, response)
  }

  @Test
  fun `test expired license check response with wrong track type`() = runTest {
    @Language("JSON")
    val responseBody = """
      {
        "trackType": "CODEFORCES"
      }
    """.trimIndent()
    configureResponse(MockResponseFactory.fromString(responseBody))

    // when
    val response = LicenseConnector.getInstance().checkLicense(link)

    // then
    assertEquals(LicenseState.ERROR, response)
  }

  @Test
  fun `test failed response to server during license check`() = runTest {
    // given
    configureResponse(MockResponseFactory.notFound())

    // when
    val response = LicenseConnector.getInstance().checkLicense(link)

    // then
    assertEquals(LicenseState.ERROR, response)
  }

  private fun configureResponse(response: MockResponse) {
    mockServer.addResponseHandler(testRootDisposable) { request, path ->
      if (request.headers["Authorization"] != "Bearer $JWT") {
        return@addResponseHandler MockResponseFactory.badRequest()
      }

      when (path) {
        "/$API_PATH" -> response
        else -> MockResponseFactory.notFound()
      }
    }
  }

  companion object {
    private const val JWT = "jwt"
    private const val API_PATH = "api/edu-track/aws/plugin/license?trackRef=red&courseRef=green&moduleRef=blue"
  }
}