package com.jetbrains.edu.learning.checkio.api.exceptions

import com.jetbrains.edu.learning.messages.EduCoreBundle.message
import retrofit2.Response

/**
 * This exception is thrown when unexpected non-2xx HTTP response is received
 * Similar to [retrofit2.HttpException], but checked
 */
class HttpException(val response: Response<*>) :
  ApiException(message("exception.message.http.info", response.code(), response.message()))
