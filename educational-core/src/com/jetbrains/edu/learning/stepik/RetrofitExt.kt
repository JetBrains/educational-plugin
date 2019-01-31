package com.jetbrains.edu.learning.stepik

import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import retrofit2.Call
import retrofit2.Response
import java.io.IOException

private val LOG = Logger.getInstance(StepikConnector::class.java.name)

fun <T> Call<T>.executeHandlingExceptions(): Response<T>? {
  try {
    return this.execute()
  }
  catch (e: IOException) {
    LOG.error("Failed to connect to server. ${e.message}")
  }
  catch (e: RuntimeException) {
    LOG.error("Failed to connect to server. ${e.message}")
  }
  return null
}

fun checkForErrors(response: Response<out Any>?) {
  if (response == null) return
  val errorBody = response.errorBody()
  if (errorBody != null) {
    LOG.error(errorBody.string())
  }
}