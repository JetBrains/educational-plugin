package com.jetbrains.edu.ai.translation.connector

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.edu.ai.host.EduAIServiceHost
import com.jetbrains.edu.ai.messages.EduAIBundle
import com.jetbrains.edu.ai.translation.service.TranslationService
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.network.HTTP_UNAVAILABLE_FOR_LEGAL_REASONS
import com.jetbrains.edu.learning.network.createRetrofitBuilder
import com.jetbrains.edu.learning.onError
import com.jetbrains.educational.core.format.enum.TranslationLanguage
import com.jetbrains.educational.translation.format.CourseTranslationResponse
import com.jetbrains.educational.translation.format.domain.TranslationVersion
import io.netty.handler.codec.http.HttpResponseStatus.UNPROCESSABLE_ENTITY
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import retrofit2.Response
import java.net.HttpURLConnection.*
import java.net.SocketException

@Service(Service.Level.APP)
class TranslationServiceConnector {
  private val aiServiceUrl: String
    get() = EduAIServiceHost.getSelectedUrl()

  private val connectionPool = ConnectionPool()

  private val service: TranslationService
    get() = translationService()

  private fun translationService(): TranslationService {
    val customInterceptor = TranslationInterceptor()
    val converterFactory = TranslationConverterFactory()

    return createRetrofitBuilder(aiServiceUrl, connectionPool, customInterceptor = customInterceptor)
      .addConverterFactory(converterFactory)
      .build()
      .create(TranslationService::class.java)
  }

  suspend fun getLatestTranslationVersion(
    marketplaceId: Int,
    updateVersion: Int,
    language: TranslationLanguage
  ): Result<TranslationVersion, String> {
    val version = networkCall {
      service
        .getLatestCourseTranslationVersion(marketplaceId, updateVersion, language.name)
        .handleResponse()
    }.onError { return Err(it) }
    if (version == null) {
      error("Translation version is null")
    }
    return Ok(version)
  }

  suspend fun getTranslatedCourse(
    marketplaceId: Int,
    updateVersion: Int,
    language: TranslationLanguage
  ): Result<CourseTranslationResponse?, String> =
    networkCall {
      service
        .getTranslatedCourse(marketplaceId, updateVersion, language.name)
        .handleResponse()
    }

  private fun <T> Response<T>.handleResponse(): Result<T?, String> {
    val code = code()
    val errorMessage = errorBody()?.string()
    if (!errorMessage.isNullOrEmpty()) {
      LOG.error("Request failed. Status code: $code. Error message: $errorMessage")
    }
    return when (code) {
      HTTP_OK -> Ok(body())
      HTTP_ACCEPTED -> Ok(null)
      HTTP_UNAVAILABLE -> Err(EduAIBundle.message("ai.translation.service.is.currently.unavailable"))
      HTTP_NOT_FOUND -> Err(EduAIBundle.message("ai.translation.course.translation.does.not.exist"))
      UNPROCESSABLE_ENTITY.code() -> Err(EduAIBundle.message("ai.translation.only.popular.courses.are.allowed.for.translation"))
      HTTP_UNAVAILABLE_FOR_LEGAL_REASONS -> Err(EduAIBundle.message("ai.translation.translation.unavailable.due.to.license.restrictions"))
      else -> Err(EduAIBundle.message("ai.translation.service.could.not.connect"))
    }
  }

  private suspend fun <T> networkCall(call: suspend () -> Result<T?, String>): Result<T?, String> =
    try {
      withContext(Dispatchers.IO) {
        call()
      }
    }
    catch (exception: SocketException) {
      Err(EduAIBundle.message("ai.translation.service.could.not.connect"))
    }

  companion object {
    private val LOG: Logger = Logger.getInstance(TranslationServiceConnector::class.java)

    fun getInstance(): TranslationServiceConnector = service()
  }
}