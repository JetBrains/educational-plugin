package com.jetbrains.edu.ai.tests

import com.jetbrains.edu.ai.tests.connector.GenerateTaskTestConverterFactory
import com.jetbrains.educational.ml.test.generation.context.TestGenerationContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.apache.http.entity.ContentType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Converter
import retrofit2.Retrofit

class GenerateTaskTestConverterFactoryTest {
  private lateinit var factory: GenerateTaskTestConverterFactory
  private lateinit var mockRetrofit: Retrofit

  @Before
  fun setUp() {
    factory = GenerateTaskTestConverterFactory()
    mockRetrofit = Retrofit.Builder().baseUrl("https://example.com").build()
  }

  @Test
  fun `test request body converter`() {
    val converter = factory.requestBodyConverter(TestGenerationContext::class.java, arrayOf(), arrayOf(), mockRetrofit)
    assertNotNull(converter)

    // Need to cast the converter to the correct type
    @Suppress("UNCHECKED_CAST")
    val typedConverter = converter as Converter<TestGenerationContext, RequestBody>

    val request = TestGenerationContext("task", "code", "prompt")
    val requestBody = typedConverter.convert(request)!!
    assertTrue(requestBody.contentType()?.toString()?.startsWith(ContentType.APPLICATION_JSON.mimeType) == true)
  }

  @Test
  fun `test response body converter`() {
    val converter = factory.responseBodyConverter(String::class.java, arrayOf(), mockRetrofit)
    assertNotNull(converter)

    val responseBody = "test response".toResponseBody("application/x-ndjson; charset=utf-8".toMediaType())
    val response = converter?.convert(responseBody)
    assertEquals("test response", response)
  }

  @Test
  fun `test request body converter returns null for unsupported type`() {
    val converter = factory.requestBodyConverter(String::class.java, arrayOf(), arrayOf(), mockRetrofit)
    assertNull(converter)
  }

  @Test
  fun `test response body converter returns null for unsupported type`() {
    val converter = factory.responseBodyConverter(Int::class.java, arrayOf(), mockRetrofit)
    assertNull(converter)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `test response body converter throws exception for invalid content type`() {
    val converter = factory.responseBodyConverter(String::class.java, arrayOf(), mockRetrofit)
    assertNotNull(converter)

    val responseBody = "test response".toResponseBody("application/json".toMediaType())
    converter?.convert(responseBody)
  }
}
