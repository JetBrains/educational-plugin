package com.jetbrains.edu.learning

import mockwebserver3.MockResponse
import okio.Buffer
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.io.InputStream
import java.net.HttpURLConnection.*

object MockResponseFactory {

  fun fromFile(path: String, responseCode: Int = HTTP_OK): MockResponse =
    fromStream(FileInputStream(path).buffered(), responseCode)

  fun fromString(data: String): MockResponse = fromStream(ByteArrayInputStream(data.toByteArray()))

  fun fromString(data: String, responseCode: Int = HTTP_OK): MockResponse =
    fromStream(ByteArrayInputStream(data.toByteArray()), responseCode)

  private fun fromStream(data: InputStream, responseCode: Int = HTTP_OK): MockResponse =
    MockResponse.Builder()
      .code(responseCode)
      .addHeader("Content-Type", "application/json; charset=utf-8")
      .body(Buffer().readFrom(data))
      .build()

  fun ok(): MockResponse = MockResponse(code = HTTP_OK)
  fun badRequest(): MockResponse = MockResponse(code = HTTP_BAD_REQUEST)
  fun notFound(): MockResponse = MockResponse(code = HTTP_NOT_FOUND)
  fun internalError(): MockResponse = MockResponse(code = HTTP_INTERNAL_ERROR)
}
