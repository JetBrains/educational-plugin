package com.jetbrains.edu.learning.marketplace.license.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.jetbrains.edu.learning.authUtils.ConnectorUtils
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.marketplace.license.LicenseState
import com.jetbrains.edu.learning.network.createRetrofitBuilder
import com.jetbrains.edu.learning.onError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.create

@Service(Service.Level.APP)
class LicenseConnector {
  val objectMapper: ObjectMapper by lazy {
    ConnectorUtils.createRegisteredMapper(SimpleModule()).apply {
      registerModule(kotlinModule())
    }
  }

  /**
   * @return Result with a boolean value indicating whether the license is active or an error message
   */
  suspend fun checkLicense(link: String): LicenseState {
    val jwtToken = MarketplaceSubmissionsConnector.getInstance().getLicenseJWT().onError {
      return LicenseState.ERROR
    }
    val connectionPool = ConnectionPool()
    val converterFactory: JacksonConverterFactory = JacksonConverterFactory.create(objectMapper)
    val service = createRetrofitBuilder(link, connectionPool, jwtToken)
      .addConverterFactory(converterFactory)
      .build()
      .create<LicenseEndpoints>()

    return try {
      val response = withContext(Dispatchers.IO) {
        service.checkLicense()
      }
      val licenseCheck = response.body()
      if (response.isSuccessful && licenseCheck != null) {
        LOG.info("Successfully checked user license")
        if (licenseCheck is LicenseCheckResponse.Ok) {
          LicenseState.VALID
        }
        else {
          LicenseState.INVALID
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

  companion object {
    fun getInstance(): LicenseConnector = service()

    private val LOG: Logger = logger<LicenseConnector>()
  }
}