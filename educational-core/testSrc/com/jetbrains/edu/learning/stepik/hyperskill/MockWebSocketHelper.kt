package com.jetbrains.edu.learning.stepik.hyperskill

import okhttp3.WebSocket

enum class MockWebSocketState {
  INITIAL, CONNECTION_CONFIRMED
}

fun WebSocket.confirmConnection() = send("Connection confirmed")

fun WebSocket.confirmSubscription() = send("Subscription confirmed")

const val webSocketConfiguration = """{"token": "fakeToken","url": "fakeUrl"}"""