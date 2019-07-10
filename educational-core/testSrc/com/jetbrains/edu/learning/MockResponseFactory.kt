package com.jetbrains.edu.learning

import okhttp3.mockwebserver.MockResponse
import okio.Buffer
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.io.InputStream

object MockResponseFactory {

  @JvmStatic
  fun fromFile(path: String): MockResponse = fromStream(FileInputStream(path).buffered())

  @JvmStatic
  fun fromString(data: String): MockResponse = fromStream(ByteArrayInputStream(data.toByteArray()))

  @JvmStatic
  fun fromStream(data: InputStream): MockResponse {
    val buffer = Buffer().readFrom(data)
    return MockResponse()
      .setResponseCode(200)
      .addHeader("Content-Type", "application/json; charset=utf-8")
      .setBody(buffer)
  }

  fun notFound(): MockResponse = MockResponse().setResponseCode(404)
}
