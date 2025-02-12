package com.jetbrains.edu.ai.tests.connector

import com.fasterxml.jackson.databind.ObjectMapper
import com.jetbrains.edu.ai.tests.service.GenerateTestRequest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.apache.http.entity.ContentType
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

class GenerateTaskTestConverterFactory : Converter.Factory() {
  private val mapper = ObjectMapper()

  override fun requestBodyConverter(
    type: Type,
    parameterAnnotations: Array<out Annotation>,
    methodAnnotations: Array<out Annotation>,
    retrofit: Retrofit
  ): Converter<*, RequestBody>? {
    if (type != GenerateTestRequest::class.java) return null

    return Converter<GenerateTestRequest, RequestBody> { request ->
      val jsonRequest = mapper.writeValueAsString(request)
      jsonRequest.toRequestBody(ContentType.APPLICATION_JSON.mimeType.toMediaType())
    }
  }
}
