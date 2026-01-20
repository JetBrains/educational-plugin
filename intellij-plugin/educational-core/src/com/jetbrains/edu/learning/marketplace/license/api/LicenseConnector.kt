package com.jetbrains.edu.learning.marketplace.license.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.marketplace.license.LicenseState
import com.jetbrains.edu.learning.network.createRetrofitBuilder
import com.jetbrains.edu.learning.onError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.create
import java.net.URI

@Service(Service.Level.APP)
class LicenseConnector {
  private val objectMapper: ObjectMapper = jacksonObjectMapper()
  private val connectionPool: ConnectionPool = ConnectionPool()

  /**
   * @return Result with a boolean value indicating whether the license is active or an error message
   */
  suspend fun checkLicense(link: String): LicenseState {
    val jwtToken = MarketplaceSubmissionsConnector.getInstance().getLicenseJWT().onError {
      return LicenseState.ERROR
    }
    val converterFactory: JacksonConverterFactory = JacksonConverterFactory.create(objectMapper)

    val (baseUrl, apiPath) = retrieveBaseUrl(link) ?: return LicenseState.ERROR

    val service = createRetrofitBuilder(baseUrl, connectionPool, jwtToken)
      .addConverterFactory(converterFactory)
      .build()
      .create<LicenseEndpoints>()

    return try {
      val response = withContext(Dispatchers.IO) {
        service.checkLicense(apiPath)
      }
      val licenseCheck = response.body()
      if (response.isSuccessful && licenseCheck != null) {
        LOG.info("Successfully checked user license")
        if (licenseCheck is LicenseCheckResponse.Ok) {
          LicenseState.VALID
        }
        else {
          LicenseState.EXPIRED
        }
      }
      else {
        val message = "Failed to check user course license: ${response.errorBody()}. Response code: ${response.code()}"
        LOG.warn(message)
        LicenseState.ERROR
      }
    }
    catch (e: Exception) {
      LOG.warn("Error occurred while checking course licence", e)
      LicenseState.ERROR
    }
  }

  private fun retrieveBaseUrl(link: String): UrlParts? {
    val uri = try {
      URI(link)
    }
    catch (e: Exception) {
      LOG.warn("Failed to parse link: $link", e)
      return null
    }
    val baseUrl = if (uri.port == -1) {
      "${uri.scheme}://${uri.host}/"
    }
    else {
      "${uri.scheme}://${uri.host}:${uri.port}/"
    }
    val apiPath = link.substringAfter(baseUrl)
    return UrlParts(baseUrl, apiPath)
  }

  private data class UrlParts(val baseUrl: String, val apiPath: String)

  companion object {
    fun getInstance(): LicenseConnector = service()

    private val LOG: Logger = logger<LicenseConnector>()
  }
}