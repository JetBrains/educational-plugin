package com.jetbrains.edu.ai.tests.connector

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.jetbrains.edu.ai.host.EduAIServiceHost
import com.jetbrains.edu.ai.tests.service.GenerateTaskTestService
import com.jetbrains.edu.learning.network.HTTP_UNAVAILABLE_FOR_LEGAL_REASONS
import com.jetbrains.edu.learning.network.createRetrofitBuilder
import com.intellij.ui.JBAccountInfoService
import com.jetbrains.educational.ml.test.generation.context.TestGenerationContext
import okhttp3.ConnectionPool
import java.net.HttpURLConnection.*

@Service(Service.Level.APP)
class GenerateTaskTestConnector {
  private val url: String
    get() = EduAIServiceHost.getSelectedUrl()

  private val connectionPool = ConnectionPool()

  private val service: GenerateTaskTestService
    get() = createGenerateTaskTestService()

  @Throws(IllegalStateException::class)
  private fun createGenerateTaskTestService(): GenerateTaskTestService {
    val userId = JBAccountInfoService.getInstance()?.userData?.id ?: run {
      LOG.error("JetBrains Account User ID is null")
      throw IllegalStateException("JetBrains Account User ID is null")
    }
    return createRetrofitBuilder(url, connectionPool, "u.$userId")
      .addConverterFactory(GenerateTaskTestConverterFactory())
      .build()
      .create(GenerateTaskTestService::class.java)
  }

  suspend fun generateTests(
    taskDescription: String,
    codeSnippet: String,
    promptCustomization: String
  ): String {
    val request = TestGenerationContext(taskDescription, codeSnippet, promptCustomization)
    val response = service.generateTests(request)

    if (!response.isSuccessful) {
      LOG.warn("Request failed. Status code: ${response.code()}. Error message: ${response.errorBody()?.string()}")
      when (response.code()) {
        HTTP_UNAVAILABLE_FOR_LEGAL_REASONS -> error("Service is unavailable for legal reasons")
        HTTP_UNAVAILABLE, HTTP_BAD_GATEWAY -> error("Service is temporarily unavailable")
        else -> error("Failed to generate tests: ${response.code()}")
      }
    }

    return response.body() ?: error("Empty response body")
  }

  companion object {
    fun getInstance(): GenerateTaskTestConnector = service()
    private val LOG: Logger = logger<GenerateTaskTestConnector>()
  }
}
