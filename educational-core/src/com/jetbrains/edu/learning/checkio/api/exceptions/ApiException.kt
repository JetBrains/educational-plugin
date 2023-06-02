package com.jetbrains.edu.learning.checkio.api.exceptions

import org.jetbrains.annotations.Nls

/**
 * Is used as marker for all exceptions related to API errors
 *
 * @see HttpException
 *
 * @see NetworkException
 *
 * @see ParseException
 *
 */
abstract class ApiException : Exception {
  constructor(message: @Nls(capitalization = Nls.Capitalization.Sentence) String) : super(message)
  constructor(cause: Throwable) : super(cause)
}
