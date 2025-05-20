package com.jetbrains.edu.ai.debugger.core.connector

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.jetbrains.edu.ai.debugger.core.host.EduAIDebuggerServiceHost
import com.jetbrains.edu.ai.debugger.core.service.AIDebuggerService
import com.jetbrains.edu.ai.debugger.core.service.BreakpointHintRequest
import com.jetbrains.edu.ai.debugger.core.service.DebuggerHintRequest
import com.jetbrains.edu.ai.debugger.core.service.TestInfo
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.network.createRetrofitBuilder
import com.jetbrains.educational.ml.debugger.dto.*
import com.jetbrains.educational.ml.debugger.request.TaskDescriptionBase
import com.jetbrains.educational.ml.debugger.response.BreakpointHintDetails
import okhttp3.ConnectionPool
import retrofit2.Response
import retrofit2.converter.jackson.JacksonConverterFactory
import java.net.HttpURLConnection.*

@Service(Service.Level.APP)
class AIDebuggerServiceConnector {
  private val url: String
    get() = EduAIDebuggerServiceHost.getSelectedUrl()

  private val connectionPool = ConnectionPool()

  private val service: AIDebuggerService
    get() = createAIDebuggerService()

  @Throws(IllegalStateException::class)
  private fun createAIDebuggerService(): AIDebuggerService {
    val objectMapper = jacksonObjectMapper()
    val factory = JacksonConverterFactory.create(objectMapper)
    return createRetrofitBuilder(url, connectionPool)
      .addConverterFactory(factory)
      .build()
      .create(AIDebuggerService::class.java)
  }

  suspend fun getBreakpointHint(
    files: Map<String, String>,
    finalBreakpoints: List<Breakpoint>,
    intermediateBreakpoints: List<Breakpoint>
  ): Result<List<BreakpointHintDetails>, String> {
    val request = BreakpointHintRequest(intermediateBreakpoints, finalBreakpoints, files)
    return service.getBreakpointHint(request).handleResponse()
  }

  suspend fun getBreakpoints(
    authorSolution: FileContentMap,
    courseId: Int,
    programmingLanguage: ProgrammingLanguage,
    taskDescription: TaskDescriptionBase,
    taskId: Int,
    testInfo: TestInfo,
    updateVersion: Int?,
    userSolution: FileContentMap
  ): Result<List<Breakpoint>, String> {
    val request = DebuggerHintRequest(
      authorSolution = authorSolution,
      courseId = courseId,
      programmingLanguage = programmingLanguage,
      taskDescription = taskDescription,
      taskId = taskId,
      testInfo = testInfo,
      updateVersion = updateVersion,
      userSolution = userSolution
    )
    return service.getBreakpoints(request).handleResponse()
  }

  private fun <T> Response<T>.handleResponse(): Result<T, String> {
    val code = code()
    if (!isSuccessful) {
      val errorBody = errorBody()?.string()
      LOG.warn("Request failed. Status code: $code. Error message: $errorBody")
      return Err(errorBody ?: "Request failed with code $code")
    }
    val responseBody = body()
    if (responseBody == null) {
      LOG.warn("Response body is null")
      return Err("Empty response received")
    }
    return Ok(responseBody)
  }

  companion object {
    private val LOG: Logger = logger<AIDebuggerServiceConnector>()

    fun getInstance(): AIDebuggerServiceConnector = service()
  }
}