package com.jetbrains.edu.learning

import okhttp3.mockwebserver.MockResponse
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

  private fun fromStream(data: InputStream, responseCode: Int = HTTP_OK): MockResponse {
    val buffer = Buffer().readFrom(data)
    return MockResponse()
      .setResponseCode(responseCode)
      .addHeader("Content-Type", "application/json; charset=utf-8")
      .setBody(buffer)
  }

  fun ok(): MockResponse = MockResponse().setResponseCode(HTTP_OK)
  fun badRequest(): MockResponse = MockResponse().setResponseCode(HTTP_BAD_REQUEST)
  fun notFound(): MockResponse = MockResponse().setResponseCode(HTTP_NOT_FOUND)
}
