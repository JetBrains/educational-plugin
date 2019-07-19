package com.jetbrains.edu.learning

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest

typealias ResponseHandler = (RecordedRequest) -> MockResponse?

class MockWebServerHelper(parentDisposable: Disposable) {

  private val handlers = mutableSetOf<ResponseHandler>()

  private val mockWebServer = MockWebServer().apply {
    dispatcher = object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        for (handler in handlers) {
          val response = handler(request)
          if (response != null) return response
        }
        return MockResponseFactory.notFound()
      }
    }
  }

  init {
    Disposer.register(parentDisposable, Disposable { mockWebServer.shutdown() })
  }

  val baseUrl: String get() = mockWebServer.url("/").toString()

  fun addResponseHandler(disposable: Disposable, handler: ResponseHandler) {
    handlers += handler
    Disposer.register(disposable, Disposable { handlers -= handler })
  }
}
