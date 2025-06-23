package com.jetbrains.edu.ai.debugger.core.connector

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.jetbrains.edu.ai.debugger.core.error.AIDebuggerServiceError
import com.jetbrains.edu.ai.debugger.core.error.BreakpointHintError
import com.jetbrains.edu.ai.debugger.core.error.BreakpointsError
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
  ): Result<List<BreakpointHintDetails>, AIDebuggerServiceError> {
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
  ): Result<List<Breakpoint>, AIDebuggerServiceError> {
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

  private inline fun <reified T> Response<List<T>>.handleResponse(): Result<List<T>, AIDebuggerServiceError> {
    val code = code()
    val errorBody = errorBody()?.string()
    if (!errorBody.isNullOrEmpty()) {
      LOG.warn("Request failed. Status code: $code. Error message: $errorBody")
    }
    val responseBody = body()
    return when {
      code == HTTP_OK && responseBody != null -> Ok(responseBody)
      code == HTTP_NO_CONTENT -> Err(getNoContentError<T>())
      else -> Err(getDefaultError<T>())
    }
  }

  private inline fun <reified T> getNoContentError(): AIDebuggerServiceError {
    return when (T::class) {
      Breakpoint::class -> BreakpointsError.NO_BREAKPOINTS
      BreakpointHintDetails::class -> BreakpointHintError.NO_BREAKPOINT_HINTS
      else -> BreakpointsError.NO_BREAKPOINTS
    }
  }

  private inline fun <reified T> getDefaultError(): AIDebuggerServiceError {
    return when (T::class) {
      BreakpointHintDetails::class -> BreakpointHintError.DEFAULT_ERROR
      else -> BreakpointsError.DEFAULT_ERROR
    }
  }

  companion object {
    private val LOG: Logger = logger<AIDebuggerServiceConnector>()

    fun getInstance(): AIDebuggerServiceConnector = service()
  }
}