package com.jetbrains.edu.learning.network

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.util.asSafely
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.flatMap
import okhttp3.Request
import okio.Timeout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.reflect.Type

/**
 * Custom implementation of Retrofit's [Call] that process network requests and
 * returns [NetworkError] in case of 4xx and 5xx response codes or typical exceptions
 *
 * The following implementation has to return successful [Response] even if underlying network request failed
 * to wrap an error into [NetworkError] and return it to an API caller
 */
internal class NetworkResultCall<T : Any>(
  private val proxy: Call<T>,
  private val paramType: Type,
) : Call<NetworkResult<T>> {

  override fun enqueue(callback: Callback<NetworkResult<T>>) {
    proxy.enqueue(object : Callback<T> {
      override fun onResponse(call: Call<T>, response: Response<T>) {
        val result = response.toNetworkResult()
        result.log()
        callback.onResponse(this@NetworkResultCall, Response.success(result))
      }

      override fun onFailure(call: Call<T>, t: Throwable) {
        if (t is ProcessCanceledException) {
          call.cancel()
        }
        val networkError = t.asSafely<Exception>()?.toNetworkError()
        if (networkError == null) {
          LOG.warn(t)
          callback.onFailure(this@NetworkResultCall, t)
        }
        else {
          val result = Err(networkError)
          result.log()
          callback.onResponse(this@NetworkResultCall, Response.success(result))
        }
      }
    })
  }

  override fun execute(): Response<NetworkResult<T>> {
    val result = proxy.executeWithCheckCanceled().flatMap { response ->
      response.toNetworkResult()
    }
    result.log()
    return Response.success(result)
  }

  override fun clone(): Call<NetworkResult<T>> = NetworkResultCall(proxy.clone(), paramType)
  override fun request(): Request = proxy.request()
  override fun timeout(): Timeout = proxy.timeout()
  override fun isExecuted(): Boolean = proxy.isExecuted
  override fun isCanceled(): Boolean = proxy.isCanceled
  override fun cancel() = proxy.cancel()

  private fun <T> Response<T>.toNetworkResult(): NetworkResult<T> {
    return transformWithErrorCodeMapping {
      // If we use `Unit` as a result type (usually expected for 204 http code),
      // `body()` will be null, and `as T` cast will fail
      @Suppress("UNCHECKED_CAST")
      if (paramType == Unit::class.java) Unit as T else body() as T
    }
  }

  private fun <T> NetworkResult<T>.log() {
    if (this is Err) {
      LOG.warn(error.message)
    }
  }

  companion object {
    private val LOG = logger<NetworkResultCall<*>>()
  }
}
