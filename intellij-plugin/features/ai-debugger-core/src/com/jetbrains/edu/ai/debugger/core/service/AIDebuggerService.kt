package com.jetbrains.edu.ai.debugger.core.service

import com.jetbrains.educational.ml.debugger.dto.BreakpointHintRequest
import com.jetbrains.educational.ml.debugger.dto.BreakpointHintResponse
import com.jetbrains.educational.ml.debugger.dto.CodeFixRequest
import com.jetbrains.educational.ml.debugger.dto.CodeFixResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AIDebuggerService{
  @POST("/api/get-breakpoint-hint")
  suspend fun getBreakpointHint(@Body request: BreakpointHintRequest): Response<BreakpointHintResponse>

  @POST("/get-code-fix")
  suspend fun getCodeFix(@Body request: CodeFixRequest): Response<CodeFixResponse>
}