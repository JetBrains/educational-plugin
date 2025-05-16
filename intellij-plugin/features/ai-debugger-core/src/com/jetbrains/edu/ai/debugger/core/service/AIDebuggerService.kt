package com.jetbrains.edu.ai.debugger.core.service

import com.jetbrains.educational.ml.debugger.context.BreakpointHintContext
import com.jetbrains.educational.ml.debugger.context.DebuggerHintContext
import com.jetbrains.educational.ml.debugger.dto.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AIDebuggerService{
  @POST("/api/get-breakpoint-hint")
  suspend fun getBreakpointHint(@Body request: BreakpointHintContext): Response<List<BreakpointHintDetails>>

  @POST("/get-breakpoints")
  suspend fun getBreakpoints(@Body request: DebuggerHintContext): Response<List<Breakpoint>>
}