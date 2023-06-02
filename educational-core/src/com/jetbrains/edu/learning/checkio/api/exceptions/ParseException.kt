package com.jetbrains.edu.learning.checkio.api.exceptions

import okhttp3.Response

/**
 * It's thrown when error occurred parsing Json object to Java object
 */
class ParseException(rawResponse: Response) : ApiException(rawResponse.toString())
