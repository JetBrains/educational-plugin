package com.jetbrains.edu.ai.tests.service

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

data class GenerateTestRequest(
  val taskDescription: String,
  val codeSnippet: String,
  val promptCustomization: String,
  val temperature: Double,
  val llmProfile: String
)

interface GenerateTaskTestService {
  @Headers("Accept: application/x-ndjson")
  @POST("/api/test-generation")
  suspend fun generateTests(@Body request: GenerateTestRequest): Response<String>
}
