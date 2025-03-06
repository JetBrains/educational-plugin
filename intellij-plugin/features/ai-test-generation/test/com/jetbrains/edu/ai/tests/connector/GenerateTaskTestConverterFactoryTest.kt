package com.jetbrains.edu.ai.tests.connector

import com.jetbrains.edu.ai.tests.service.GenerateTestRequest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.apache.http.entity.ContentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GenerateTaskTestConverterFactoryTest {
  private val factory = GenerateTaskTestConverterFactory()

  @Test
  fun `test request body converter`() {
    val converter = factory.requestBodyConverter(GenerateTestRequest::class.java, arrayOf(), arrayOf(), null)
    assertNotNull(converter)

    val request = GenerateTestRequest("task", "code", "prompt", 0.7, "default")
    val requestBody = converter?.convert(request) as RequestBody
    assertEquals(ContentType.APPLICATION_JSON.mimeType, requestBody.contentType()?.toString())
  }

  @Test
  fun `test response body converter`() {
    val converter = factory.responseBodyConverter(String::class.java, arrayOf(), null)
    assertNotNull(converter)

    val responseBody = "test response".toResponseBody("application/x-ndjson".toMediaType())
    val response = converter?.convert(responseBody)
    assertEquals("test response", response)
  }

  @Test
  fun `test request body converter returns null for unsupported type`() {
    val converter = factory.requestBodyConverter(String::class.java, arrayOf(), arrayOf(), null)
    assertNull(converter)
  }

  @Test
  fun `test response body converter returns null for unsupported type`() {
    val converter = factory.responseBodyConverter(Int::class.java, arrayOf(), null)
    assertNull(converter)
  }
}
