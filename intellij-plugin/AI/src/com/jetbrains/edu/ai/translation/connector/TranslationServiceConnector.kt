package com.jetbrains.edu.ai.translation.connector

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.util.concurrency.SynchronizedClearableLazy
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
import retrofit2.converter.jackson.JacksonConverterFactory

@Suppress("unused")
@Service(Service.Level.APP)
class TranslationServiceConnector(private val scope: CoroutineScope) {
  private val aiServiceUrl: String
    get() = EduAIServiceHost.getSelectedUrl()

  private val connectionPool = ConnectionPool()

  private val service: TranslationService
    get() = serviceHolder.value

  private val serviceHolder = SynchronizedClearableLazy {
    val objectMapper = jacksonObjectMapper()
    val converterFactory = JacksonConverterFactory.create(objectMapper)

    createRetrofitBuilder(aiServiceUrl, connectionPool)
      .addConverterFactory(converterFactory)
      .build()
      .create(TranslationService::class.java)
  }

  suspend fun getTranslatedCourse(
    marketplaceId: MarketplaceId,
    updateVersion: UpdateVersion,
    language: Language
  ): Result<CourseTranslation, String> {
    val response = scope
      .async(Dispatchers.IO) {
        service
          .getTranslatedCourse(marketplaceId.value, updateVersion.value, language.name)
          .executeParsingErrors()
      }
      .await()
      .onError {
        LOG.error("Failed to get translation for course ($marketplaceId, $updateVersion, $language). Error message: $it")
        return Err(it)
      }

    return when (val courseTranslation = response.body()) {
      null -> Err("Response body is null")
      else -> Ok(courseTranslation)
    }
  }

  suspend fun getTranslatedTask(
    marketplaceId: MarketplaceId,
    updateVersion: UpdateVersion,
    language: Language,
    taskId: TaskEduId
  ): Result<CourseTranslation, String> {
    val response = scope
      .async(Dispatchers.IO) {
        service
          .getTranslatedTask(marketplaceId.value, updateVersion.value, language.name, taskId.value)
          .executeParsingErrors()
      }
      .await()
      .onError {
        LOG.error("Failed to get translation for course ($marketplaceId, $updateVersion, $language, $taskId). Error message: $it")
        return Err(it)
      }

    return when (val courseTranslation = response.body()) {
      null -> Err("Response body is null")
      else -> Ok(courseTranslation)
    }
  }

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
        LOG.error("Failed to translate course ($marketplaceId, $updateVersion, $language, force: $force)")
        return Err(it)
      }

    return when (response.isSuccessful) {
      true -> Ok(true)
      false -> Err("Response body is null")
    }
  }

  companion object {
    private val LOG: Logger = thisLogger()

    fun getInstance(): TranslationServiceConnector = service()
  }
}