package com.jetbrains.edu.learning.checkio.api.exceptions

import com.jetbrains.edu.learning.messages.EduCoreBundle.message
import java.io.IOException

/**
 * This exception is thrown when network error occurred,
 * e.g. internet connection is disabled or endpoint doesn't respond
 */
class NetworkException : ApiException {
  constructor() : super(message("exception.message.connection.failed"))

  constructor(e: IOException) : super(e)
}
