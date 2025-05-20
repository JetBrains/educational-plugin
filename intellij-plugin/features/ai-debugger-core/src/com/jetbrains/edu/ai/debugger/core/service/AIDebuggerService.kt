package com.jetbrains.edu.ai.debugger.core.service

import com.jetbrains.educational.ml.debugger.dto.Breakpoint
import com.jetbrains.educational.ml.debugger.response.BreakpointHintDetails
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AIDebuggerService{
  @POST("/api/get-breakpoint-hint")
  suspend fun getBreakpointHint(@Body request: BreakpointHintRequest): Response<List<BreakpointHintDetails>>

  @POST("/api/get-breakpoints")
  suspend fun getBreakpoints(@Body request: DebuggerHintRequest): Response<List<Breakpoint>>
}