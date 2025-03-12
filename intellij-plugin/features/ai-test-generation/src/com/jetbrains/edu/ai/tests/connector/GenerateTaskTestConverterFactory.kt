package com.jetbrains.edu.ai.tests.connector

import com.fasterxml.jackson.databind.ObjectMapper
import com.jetbrains.educational.ml.test.generation.context.TestGenerationContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.apache.http.entity.ContentType
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

class GenerateTaskTestConverterFactory : Converter.Factory() {
  private val mapper = ObjectMapper()

  override fun responseBodyConverter(
    type: Type,
    annotations: Array<out Annotation>,
    retrofit: Retrofit
  ): Converter<ResponseBody, String>? {
    if (type != String::class.java) return null

    return Converter<ResponseBody, String> { body ->
      if (body.contentType()?.toString()?.startsWith("application/x-ndjson") != true) {
        throw IllegalArgumentException("Expected application/x-ndjson content type but got ${body.contentType()}")
      }
      body.string()
    }
  }

  override fun requestBodyConverter(
    type: Type,
    parameterAnnotations: Array<out Annotation>,
    methodAnnotations: Array<out Annotation>,
    retrofit: Retrofit
  ): Converter<*, RequestBody>? {
    if (type != TestGenerationContext::class.java) return null

    return Converter<TestGenerationContext, RequestBody> { request ->
      val jsonRequest = mapper.writeValueAsString(request)
      jsonRequest.toRequestBody(ContentType.APPLICATION_JSON.mimeType.toMediaType())
    }
  }
}
