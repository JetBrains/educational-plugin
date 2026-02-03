package com.jetbrains.edu.learning.network

import java.net.HttpURLConnection.HTTP_BAD_GATEWAY
import java.net.HttpURLConnection.HTTP_CLIENT_TIMEOUT
import java.net.HttpURLConnection.HTTP_GATEWAY_TIMEOUT
import java.net.HttpURLConnection.HTTP_INTERNAL_ERROR
import java.net.HttpURLConnection.HTTP_UNAVAILABLE
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

data class RetryPolicy(
  val retryAttempts: Int = 3,
  val initialDelay: Duration = 500.milliseconds,
  val maxDelay: Duration = 5.seconds,
  // adds some randomness to the backoff to spread the retries around in time
  val jitter: Double = 0.2,
  val retryStatusCodes: Set<Int> = setOf(
    HTTP_CLIENT_TIMEOUT,
    HTTP_INTERNAL_ERROR,
    HTTP_BAD_GATEWAY,
    HTTP_UNAVAILABLE,
    HTTP_GATEWAY_TIMEOUT
  )
)
