package com.jetbrains.edu.learning.stepik.hyperskill.api

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.jetbrains.edu.learning.MockResponseFactory
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest

typealias ResponseHandler = (RecordedRequest) -> MockResponse?

class MockHyperskillConnector : HyperskillConnector(), Disposable {

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

  private val handlers = mutableSetOf<ResponseHandler>()

  init {
    Disposer.register(ApplicationManager.getApplication(), this)
  }

  override val baseUrl: String get() = mockWebServer.url("/").toString()

  fun withResponseHandler(disposable: Disposable, handler: ResponseHandler): MockHyperskillConnector {
    handlers += handler
    Disposer.register(disposable, Disposable { handlers -= handler })
    return this
  }

  override fun dispose() {
    mockWebServer.shutdown()
  }
}
