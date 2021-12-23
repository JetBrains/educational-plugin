package com.jetbrains.edu.learning.checkio.api

import com.intellij.openapi.diagnostic.logger
import com.jetbrains.edu.learning.checkio.api.exceptions.HttpException
import com.jetbrains.edu.learning.checkio.api.exceptions.NetworkException
import com.jetbrains.edu.learning.checkio.api.exceptions.ParseException
import com.jetbrains.edu.learning.checkio.connectors.CheckiOApiConnector
import retrofit2.Call
import java.io.IOException

private val LOG = logger<CheckiOApiConnector>()

fun <T> Call<T>.executeHandlingCheckiOExceptions(): T {
  LOG.info("Executing request: " + request().toString())

  return try {
    val response = execute()
    if (!response.isSuccessful) {
      throw HttpException(response)
    }
    val body = response.body() ?: throw ParseException(response.raw())
    body
  }
  catch (e: IOException) {
    throw NetworkException(e)
  }
}