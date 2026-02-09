package com.jetbrains.edu.learning.network

import retrofit2.Call
import retrofit2.CallAdapter
import java.lang.reflect.Type

internal class NetworkResultCallAdapter<T : Any>(
  private val resultType: Type,
  private val paramType: Type,
) : CallAdapter<T, Call<NetworkResult<T>>> {

  override fun responseType(): Type = resultType

  override fun adapt(call: Call<T>): Call<NetworkResult<T>> {
    return NetworkResultCall(call, paramType)
  }
}
