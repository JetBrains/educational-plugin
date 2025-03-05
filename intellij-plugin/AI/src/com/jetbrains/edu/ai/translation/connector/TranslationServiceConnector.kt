package com.jetbrains.edu.ai.translation.connector

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.jetbrains.edu.ai.connector.AIServiceConnector
import com.jetbrains.edu.ai.error.AIServiceError
import com.jetbrains.edu.ai.error.CommonAIServiceError
import com.jetbrains.edu.ai.translation.TranslationError
import com.jetbrains.edu.ai.translation.service.TranslationService
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.network.HTTP_UNAVAILABLE_FOR_LEGAL_REASONS
import com.jetbrains.edu.learning.network.createRetrofitBuilder
import com.jetbrains.educational.core.format.enum.TranslationLanguage
import com.jetbrains.educational.translation.format.CourseTranslationResponse
import com.jetbrains.educational.translation.format.domain.TranslationVersion
import retrofit2.converter.jackson.JacksonConverterFactory
import java.net.HttpURLConnection.*

@Service(Service.Level.APP)
class TranslationServiceConnector : AIServiceConnector() {
  private val service: TranslationService
    get() = translationService()

  private fun translationService(): TranslationService {
    val objectMapper = jacksonObjectMapper()
    val factory = JacksonConverterFactory.create(objectMapper)
    return createRetrofitBuilder(aiServiceUrl, connectionPool)
      .addConverterFactory(factory)
      .build()
      .create(TranslationService::class.java)
  }

  suspend fun getLatestTranslationVersion(
    marketplaceId: Int,
    updateVersion: Int,
    language: TranslationLanguage
  ): Result<TranslationVersion, AIServiceError> =
    networkCall {
      service
        .getLatestCourseTranslationVersion(marketplaceId, updateVersion, language.name)
        .handleResponse()
    }

  suspend fun getTranslatedCourse(
    marketplaceId: Int,
    updateVersion: Int,
    language: TranslationLanguage
  ): Result<CourseTranslationResponse, AIServiceError> =
    networkCall {
      service
        .getTranslatedCourse(marketplaceId, updateVersion, language.name)
        .handleResponse()
    }

  override fun <T> parseResponseCode(code: Int, result: T?): Result<T, AIServiceError> {
    return when {
      code == HTTP_OK && result != null -> Ok(result)
      code == HTTP_NOT_FOUND -> TranslationError.NO_TRANSLATION.asErr()
      code == HTTP_UNAVAILABLE_FOR_LEGAL_REASONS -> TranslationError.TRANSLATION_UNAVAILABLE_FOR_LEGAL_REASONS.asErr()
      code == HTTP_UNAVAILABLE || result == null -> CommonAIServiceError.SERVICE_UNAVAILABLE.asErr()
      else -> CommonAIServiceError.CONNECTION_ERROR.asErr()
    }
  }

  companion object {
    fun getInstance(): TranslationServiceConnector = service()
  }
}