package com.jetbrains.edu.learning.network

import com.jetbrains.edu.learning.courseFormat.logger
import okhttp3.Interceptor
import okhttp3.Response
import kotlin.math.pow
import kotlin.random.Random

class RetryInterceptor(private val retryPolicy: RetryPolicy) : Interceptor {

  override fun intercept(chain: Interceptor.Chain): Response {
    var retries = 0

    while (true) {
      val request = chain.request()
      LOG.info("${request.method} ${request.url}. Attempt: ${retries + 1}")

      val response = chain.proceed(request)

      if (response.code !in retryPolicy.retryStatusCodes || retries >= retryPolicy.retryAttempts) {
        return response
      }

      response.close()

      // Apply exponential backoff with jitter
      val multiplier = 2.0.pow(retries)
      val jitterFactor = 1.0 + Random.nextDouble(-retryPolicy.jitter, retryPolicy.jitter)
      val sleepFor = (retryPolicy.initialDelay * multiplier * jitterFactor).coerceAtMost(retryPolicy.maxDelay)

      // Not the ideal solution if a request is made with suspend function.
      // Unfortunately, okhttp knows nothing about suspend functions and coroutine context, so let's use old `Thread.sleep` for now
      Thread.sleep(sleepFor.inWholeMilliseconds)

      retries++
    }
  }

  companion object {
    private val LOG = logger("com.jetbrains.edu.learning.network.RetryInterceptor")
  }
}
