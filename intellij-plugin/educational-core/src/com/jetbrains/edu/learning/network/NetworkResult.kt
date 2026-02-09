package com.jetbrains.edu.learning.network

import com.jetbrains.edu.learning.Result

typealias NetworkResult<T> = Result<T, NetworkError>

/**
 * Represents an unsuccessful result of network request execution
 */
sealed class NetworkError {

  abstract val message: String
  /**
   * Represents non-successful (i.e. when response code >= 400) response
   */
  data class HttpError(val errorCode: Int, override val message: String) : NetworkError()

  /**
   * Represents typical exceptions during network request execution
   */
  data class Exception(val title: String, val exceptionMessage: String? = null) : NetworkError() {
    constructor(title: String, exception: kotlin.Exception) : this(title, exception.message)

    override val message: String = if (exceptionMessage == null) title else "$title\n\n${exceptionMessage}"
  }
}
