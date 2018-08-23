package com.jetbrains.edu.learning.checkio.api.exceptions;

import com.jetbrains.edu.learning.checkio.call.CheckiOCall;
import org.jetbrains.annotations.NotNull;
import retrofit2.Response;

/**
 * This exception is thrown when unexpected non-2xx HTTP response is received
 * Similar to {@link retrofit2.HttpException}, but checked
 *
 * @see CheckiOCall#execute()
 * */
public class HttpException extends ApiException {
  public HttpException(@NotNull Response<?> response) {
    super("HTTP " + response.code() + " " + response.message());
  }
}
