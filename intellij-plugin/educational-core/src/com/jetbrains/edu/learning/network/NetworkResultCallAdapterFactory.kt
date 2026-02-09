package com.jetbrains.edu.learning.network

import com.jetbrains.edu.learning.Result
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Retrofit
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * Adds support for [NetworkResult] as response body type of [Call].
 *
 * In this case, [Call.execute] handles expected execution exception and non-successful http responses
 * wrapping them into [NetworkError].
 *
 * Supports both synchronous and asynchronous (including the suspend version) calls
 */
object NetworkResultCallAdapterFactory : CallAdapter.Factory() {

  override fun get(returnType: Type, annotations: Array<out Annotation>, retrofit: Retrofit): CallAdapter<*, *>? {
    if (getRawType(returnType) != Call::class.java) return null

    val innerType = getParameterUpperBound(0, returnType as ParameterizedType)
    if (getRawType(innerType) != Result::class.java) return null

    val okType = getParameterUpperBound(0, innerType as ParameterizedType)
    val errorType = getParameterUpperBound(1, innerType)

    if (getRawType(errorType) != NetworkError::class.java) return null

    val paramType = getRawType(okType)
    return NetworkResultCallAdapter<Any>(
      resultType = okType,
      paramType = paramType,
    )
  }
}
