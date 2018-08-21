package com.jetbrains.edu.learning.checkio.api.exceptions;

import com.jetbrains.edu.learning.checkio.api.CheckiOResponse;
import com.jetbrains.edu.learning.checkio.api.RetrofitUtils;
import org.jetbrains.annotations.NotNull;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;

/**
 * This exception is thrown when unexpected non-2xx HTTP response is received
 * Similar to {@link retrofit2.HttpException}, but checked
 *
 * @see CheckiOResponse#createNetworkError(IOException)
 * @see RetrofitUtils#getResponse(Call)
 * */
public class HttpException extends ApiException {
  public HttpException(@NotNull Response<?> response) {
    super("HTTP " + response.code() + " " + response.message());
  }
}
