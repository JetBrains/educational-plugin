package com.jetbrains.edu.learning.marketplace.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.util.io.isLocalHost
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.marketplace.changeHost.LearningCenterServiceHost
import com.jetbrains.edu.learning.network.NetworkResult
import com.jetbrains.edu.learning.network.createRetrofitBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import org.jetbrains.annotations.VisibleForTesting
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.create
import java.net.URI

@Service(Service.Level.APP)
class LearningCenterConnector {
  private val connectionPool: ConnectionPool = ConnectionPool()
  private val objectMapper: ObjectMapper = jacksonObjectMapper()
  private val converterFactory: JacksonConverterFactory = JacksonConverterFactory.create(objectMapper)

  private val learningCenterUrl: String
    get() = LearningCenterServiceHost.selectedHost.url


  @get:VisibleForTesting
  internal val isLocalInstance: Boolean
    get() {
      val host = runCatching { URI(learningCenterUrl).host }.getOrNull() ?: return false
      return isLocalHost(host)
    }

  private fun learningCenterEndpoints(authorization: Authorization? = null): LearningCenterEndpoints =
    createRetrofitBuilder(
      learningCenterUrl,
      connectionPool,
      accessToken = authorization?.credentials,
      authHeaderName = authorization?.headerName ?: AUTHORIZATION_HEADER,
      authHeaderValue = authorization?.headerValuePrefix
    )
      .addConverterFactory(converterFactory)
      .build()
      .create<LearningCenterEndpoints>()

  suspend fun tryIssueCertificate(completedPercent: Int, courseId: Int): Result<CourseCertificateIssueResponse, String> {
    val authorization = withContext(Dispatchers.IO) { authorization() }.onError { return Err(it) }

    return request("issue a certificate for course $courseId") {
      learningCenterEndpoints(authorization).tryIssueCertificate(completedPercent, courseId)
    }
  }

  suspend fun courseCertificationSupport(courseId: Int): Result<CourseCertificationSupportResponse, String> {
    return request("check certification support for course $courseId") {
      learningCenterEndpoints().courseHasCertificate(courseId)
    }
  }

  /**
   * Returns the way the requests should be authorized, or an error if the user credentials can't be obtained
   */
  private suspend fun authorization(): Result<Authorization, String> {
    return if (isLocalInstance) {
      val email = MarketplaceConnector.getInstance().getCurrentUserInfo()?.email?.takeIf { it.isNotBlank() }
                  ?: return Err("Failed to obtain JetBrains Account email")
      Ok(Authorization(DEV_AUTH_HEADER, headerValuePrefix = null, credentials = email))
    }
    else {
      MarketplaceSubmissionsConnector.getInstance().getUserJWT().map { jwtToken ->
        Authorization(AUTHORIZATION_HEADER, headerValuePrefix = BEARER, credentials = jwtToken)
      }
    }
  }

  private suspend fun <T> request(actionDescription: String, call: suspend () -> NetworkResult<T>): Result<T, String> {
    return withContext(Dispatchers.IO) { call() }
      .mapErr { "Error occurred while trying to $actionDescription" }
  }

  private class Authorization(val headerName: String, val headerValuePrefix: String?, val credentials: String)

  companion object {
    private const val AUTHORIZATION_HEADER: String = "Authorization"
    private const val BEARER: String = "Bearer"

    /**
     * A locally running Learning Center instance identifies the user by email instead of a JetBrains Account token
     */
    private const val DEV_AUTH_HEADER: String = "X-Dev-Auth"

    fun getInstance(): LearningCenterConnector = service()
  }
}
