package com.jetbrains.edu.learning

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.common.ThreadLeakTracker
import com.intellij.util.net.ProxyConfiguration
import com.intellij.util.net.ProxyConfiguration.ProxyProtocol
import com.jetbrains.edu.learning.network.USER_AGENT
import com.jetbrains.edu.learning.network.eduToolsUserAgent
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.junit.Assert.assertEquals

typealias ResponseHandler = (RecordedRequest, String) -> MockResponse?

class MockWebServerHelper(parentDisposable: Disposable) {

  private val handlers = mutableSetOf<ResponseHandler>()

  private val mockWebServer = MockWebServer().apply {
    dispatcher = object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        if (expectEduToolsUserAgent(request)) {
          assertEquals(eduToolsUserAgent, request.headers[USER_AGENT])
        }
        val path = request.target
        for (handler in handlers) {
          val response = handler(request, path)
          if (response != null) return response
        }
        return MockResponseFactory.notFound()
      }
    }
  }

  init {
    mockWebServer.start()
    Disposer.register(parentDisposable) { mockWebServer.close() }
    ThreadLeakTracker.longRunningThreadCreated(parentDisposable, "MockWebServer", "OkHttp TaskRunner", "Okio Watchdog")
  }

  val baseUrl: String get() = mockWebServer.url("/").toString()

  fun addResponseHandler(disposable: Disposable, handler: ResponseHandler) {
    handlers += handler
    Disposer.register(disposable) { handlers -= handler }
  }

  // DownloadUtil.downloadAtomically(), used in com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector.loadCourseStructure(),
  // sets product name as user agent, so such requests are not expected to contain eduToolsUserAgent
  private fun expectEduToolsUserAgent(request: RecordedRequest): Boolean = !request.pathWithoutPrams.contains("plugin")

  /**
   * Return proxy configuration to use running mock web server as a proxy
   */
  fun proxyConfiguration(): ProxyConfiguration.StaticProxyConfiguration {
    return ProxyConfiguration.proxy(ProxyProtocol.HTTP, mockWebServer.hostName, mockWebServer.port)
  }
}

fun RecordedRequest.hasParams(vararg params: Pair<String, String>): Boolean {
  return params.all { param -> url.queryParameter(param.first) == param.second }
}

val RecordedRequest.pathWithoutPrams: String
  get() = url.toUrl().path

val RecordedRequest.bodyAsString: String
  get() = body?.utf8().orEmpty()
