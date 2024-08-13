package com.jetbrains.edu.ai.translation.connector

import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.net.HttpURLConnection.HTTP_ACCEPTED

class TranslationInterceptor : Interceptor {
  override fun intercept(chain: Chain): Response {
    val response = chain.proceed(chain.request())
    if (response.code == HTTP_ACCEPTED) {
      // If the response code is 202, create and return a new response indicating this
      return response.newBuilder().body("".toResponseBody(response.body?.contentType())).build()
    }
    return response
  }
}