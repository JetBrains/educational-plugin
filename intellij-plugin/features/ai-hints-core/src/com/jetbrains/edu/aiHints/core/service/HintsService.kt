package com.jetbrains.edu.aiHints.core.service

import com.jetbrains.educational.ml.hints.context.CodeHintContext
import com.jetbrains.educational.ml.hints.context.TextHintContext
import com.jetbrains.educational.ml.hints.hint.CodeHint
import com.jetbrains.educational.ml.hints.hint.TextHint
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface HintsService {
  @POST("/api/hints/code")
  suspend fun getCodeHint(@Body context: CodeHintContext): Response<List<CodeHint>>

  @POST("/api/hints/text")
  suspend fun getTextHint(@Body context: TextHintContext): Response<List<TextHint>>
}