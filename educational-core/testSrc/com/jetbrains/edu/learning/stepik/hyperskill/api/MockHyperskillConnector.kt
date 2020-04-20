package com.jetbrains.edu.learning.stepik.hyperskill.api

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.MockWebServerHelper
import com.jetbrains.edu.learning.ResponseHandler
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class MockHyperskillConnector : HyperskillConnector() {

  private val helper = MockWebServerHelper(ApplicationManager.getApplication())

  override val baseUrl: String get() = helper.baseUrl

  private var webSocketListener: WebSocketListener? = null

  fun withResponseHandler(disposable: Disposable, handler: ResponseHandler): MockHyperskillConnector {
    helper.addResponseHandler(disposable, handler)
    return this
  }

  fun withWebSocketListener(listener: WebSocketListener) {
    webSocketListener = listener
  }

  override fun createWebSocket(client: OkHttpClient, url: String, listener: WebSocketListener): WebSocket {
    val webSocketMockSever = helper.webSocketMockSever
    val webSocket = client.newWebSocket(Request.Builder().url(webSocketMockSever.url("/")).build(), listener)
    webSocketMockSever.enqueue(MockResponseFactory.fromString("Mock Server Started").withWebSocketUpgrade(webSocketListener))
    return webSocket
  }
}
