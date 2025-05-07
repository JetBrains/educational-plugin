package com.jetbrains.edu.ai.debugger.core.connector

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.jetbrains.edu.ai.debugger.core.host.EduAIDebuggerServiceHost
import com.jetbrains.edu.ai.debugger.core.service.AIDebuggerService
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.network.createRetrofitBuilder
import com.jetbrains.educational.ml.debugger.dto.*
import okhttp3.ConnectionPool
import retrofit2.Response
import java.net.HttpURLConnection.*

@Service(Service.Level.APP)
class AIDebuggerServiceConnector {
  private val url: String
    get() = EduAIDebuggerServiceHost.getSelectedUrl()

  private val connectionPool = ConnectionPool()

  private val service: AIDebuggerService
    get() = createAIDebuggerService()

  @Throws(IllegalStateException::class)
  private fun createAIDebuggerService(): AIDebuggerService =
    createRetrofitBuilder(url, connectionPool)
      .build()
      .create(AIDebuggerService::class.java)

  suspend fun getBreakpointHint(
    files: Map<String, String>,
    finalBreakpoints: List<FinalBreakpoint>,
    intermediateBreakpoints: List<IntermediateBreakpoint>
  ): Result<BreakpointHintResponse, String> {
    val solution = files.map { FileContent(it.key, it.value) }
    val request = BreakpointHintRequest(solution, finalBreakpoints, intermediateBreakpoints)
    return service.getBreakpointHint(request).handleResponse()
  }

  suspend fun getCodeFix(
    taskDescription: TaskDescription,
    files: Map<String, String>,
    testDescription: String
  ): Result<CodeFixResponse, String> {
    val solution = files.map { FileContent(it.key, it.value) }
    val request = CodeFixRequest(taskDescription, solution, testDescription)
    return service.getCodeFix(request).handleResponse()
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