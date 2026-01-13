package com.jetbrains.edu.learning.marketplace.license

import com.intellij.util.application
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.marketplace.license.api.LicenseCheckResponse
import com.jetbrains.edu.learning.marketplace.license.api.LicenseConnector
import com.jetbrains.edu.learning.marketplace.license.api.LicenseEndpoints
import com.jetbrains.edu.learning.marketplace.metadata.getRandomTrustedUrl
import com.jetbrains.edu.learning.mockService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import org.intellij.lang.annotations.Language
import retrofit2.Response
import retrofit2.Retrofit
import kotlin.test.Test
import kotlin.test.assertFails
import kotlinx.coroutines.test.runTest

class LicenseCheckResponseTest : EduTestCase() {
  @Test
  fun `test successful license check response`() = runTest {
    configureResponse(@Language("JSON") "{}")
    val response = LicenseConnector.getInstance().checkLicense(getRandomTrustedUrl())
    assertEquals(LicenseState.VALID, response)
  }

  @Test
  fun `test expired license check response`() = runTest {
    configureResponse(@Language("JSON") """
      {
        "trackType": "AWS"
      }
    """.trimIndent())
    val response = LicenseConnector.getInstance().checkLicense(getRandomTrustedUrl())
    assertEquals(LicenseState.INVALID, response)
  }

  @Test
  fun `test successful license check response deserializes correctly`() = runTest {
    val responseBody = @Language("JSON") "{}"

    val mapper = LicenseConnector.getInstance().objectMapper
    val response = mapper.treeToValue(mapper.readTree(responseBody), LicenseCheckResponse::class.java)
    assertEquals(LicenseCheckResponse.Ok, response)
  }

  @Test
  fun `test expired aws license check response deserializes correctly`() = runTest {
    val responseBody = @Language("JSON") """
      {
        "trackType": "AWS"
      }
    """.trimIndent()

    val mapper = LicenseConnector.getInstance().objectMapper
    val response = mapper.treeToValue(mapper.readTree(responseBody), LicenseCheckResponse::class.java)
    assertEquals(LicenseCheckResponse.Error(LicenseCheckResponse.Error.TrackType.AWS), response)
  }

  @Test
  fun `test expired license check response with wrong track type check fails during deserialization`() = runTest {
    val responseBody = @Language("JSON") """
      {
        "trackType": "CODEFORCES"
      }
    """.trimIndent()

    val mapper = LicenseConnector.getInstance().objectMapper
    assertFails {
      mapper.treeToValue(mapper.readTree(responseBody), LicenseCheckResponse::class.java)
    }
  }

  private fun configureResponse(responseBody: String) {
    mockkConstructor(Retrofit::class)
    val endpoints = mockk<LicenseEndpoints>()
    every {
      anyConstructed<Retrofit>().create(LicenseEndpoints::class.java)
    } returns endpoints

    val marketplaceSubmissionsConnector = mockService<MarketplaceSubmissionsConnector>(application)
    coEvery { marketplaceSubmissionsConnector.getLicenseJWT() } returns Ok("jwt")

    val mapper = LicenseConnector.getInstance().objectMapper

    val responseBody = mapper.treeToValue(mapper.readTree(responseBody), LicenseCheckResponse::class.java)
    coEvery { endpoints.checkLicense() } returns Response.success(responseBody)
  }
}