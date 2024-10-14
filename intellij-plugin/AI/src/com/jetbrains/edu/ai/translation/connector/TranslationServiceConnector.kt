package com.jetbrains.edu.ai.translation.connector

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.thisLogger
import com.jetbrains.edu.ai.host.EduAIServiceHost
import com.jetbrains.edu.ai.translation.service.TranslationService
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.network.createRetrofitBuilder
import com.jetbrains.edu.learning.network.executeParsingErrors
import com.jetbrains.edu.learning.onError
import com.jetbrains.educational.core.format.domain.MarketplaceId
import com.jetbrains.educational.core.format.domain.TaskEduId
import com.jetbrains.educational.core.format.domain.UpdateVersion
import com.jetbrains.educational.translation.enum.Language
import com.jetbrains.educational.translation.format.CourseTranslation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import okhttp3.ConnectionPool
import retrofit2.Response
import java.net.HttpURLConnection.HTTP_ACCEPTED

@Suppress("unused")
@Service(Service.Level.APP)
class TranslationServiceConnector(private val scope: CoroutineScope) {
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

  suspend fun getTranslatedCourse(
    marketplaceId: MarketplaceId,
    updateVersion: UpdateVersion,
    language: Language
  ): Result<CourseTranslation?, String> =
    scope
      .async(Dispatchers.IO) {
        service
          .getTranslatedCourse(marketplaceId.value, updateVersion.value, language.name)
          .executeGetCall()
      }
      .await()

  suspend fun getTranslatedTask(
    marketplaceId: MarketplaceId,
    updateVersion: UpdateVersion,
    language: Language,
    taskId: TaskEduId
  ): Result<CourseTranslation?, String> =
    scope
      .async(Dispatchers.IO) {
        service
          .getTranslatedTask(marketplaceId.value, updateVersion.value, language.name, taskId.value)
          .executeGetCall()
      }
      .await()

  suspend fun doTranslateCourse(
    marketplaceId: MarketplaceId,
    updateVersion: UpdateVersion,
    language: Language,
    force: Boolean = false
  ): Result<Boolean, String> {
    val response = scope
      .async(Dispatchers.IO) {
        service
          .translateCourse(marketplaceId.value, updateVersion.value, language.name, force)
          .executeParsingErrors()
      }
      .await()
      .onError {
        return Err(it)
      }
      .onAccepted {
        return Ok(false)
      }

    return when (response.isSuccessful) {
      true -> Ok(true)
      false -> Err("Response body is null")
    }
  }

  private fun <T> Response<T>.executeGetCall(): Result<T?, String> {
    val response = executeParsingErrors()
      .onError {
        return Err(it)
      }
      .onAccepted {
        return Ok(null)
      }
    return when (val body = response.body()) {
      null -> Err("Response body is null")
      else -> Ok(body)
    }
  }

  private inline fun <T> Response<T>.onAccepted(action: () -> Unit): Response<T> {
    if (code() == HTTP_ACCEPTED) {
      LOG.info("Translation service is currently preparing your course translation. Please check back later.")
      action()
    }
    return this
  }

  companion object {
    private val LOG: Logger = thisLogger()

    fun getInstance(): TranslationServiceConnector = service()
  }
}