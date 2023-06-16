package com.jetbrains.edu.learning

import okhttp3.mockwebserver.MockResponse
import okio.Buffer
import org.apache.http.HttpStatus
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.io.InputStream

object MockResponseFactory {

  fun fromFile(path: String, responseCode: Int = HttpStatus.SC_OK): MockResponse = fromStream(FileInputStream(path).buffered(),
                                                                                              responseCode)

  fun fromString(data: String): MockResponse = fromStream(ByteArrayInputStream(data.toByteArray()))

  fun fromString(data: String, responseCode: Int = HttpStatus.SC_OK): MockResponse = fromStream(ByteArrayInputStream(data.toByteArray()),
                                                                                                responseCode)

  fun fromStream(data: InputStream, responseCode: Int = HttpStatus.SC_OK): MockResponse {
    val buffer = Buffer().readFrom(data)
    return MockResponse()
      .setResponseCode(responseCode)
      .addHeader("Content-Type", "application/json; charset=utf-8")
      .setBody(buffer)
  }

  fun notFound(): MockResponse = MockResponse().setResponseCode(404)
}
