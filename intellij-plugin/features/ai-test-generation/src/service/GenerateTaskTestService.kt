package com.jetbrains.edu.ai.tests.service

import com.jetbrains.educational.ml.test.generation.context.TestGenerationContext
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface GenerateTaskTestService {
  @POST("/api/test-generation")
  suspend fun generateTests(@Body request: TestGenerationContext): Response<String>
}
