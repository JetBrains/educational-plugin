package com.jetbrains.edu.learning.checkio.api.exceptions;

import com.jetbrains.edu.learning.checkio.api.CheckiORetrofitExtKt;
import com.jetbrains.edu.learning.messages.EduCoreBundle;
import org.jetbrains.annotations.NotNull;
import retrofit2.Response;

/**
 * This exception is thrown when unexpected non-2xx HTTP response is received
 * Similar to {@link retrofit2.HttpException}, but checked
 *
 * @see CheckiORetrofitExtKt#executeHandlingCheckiOExceptions(retrofit2.Call)
 * */
public class HttpException extends ApiException {
  @NotNull private final Response<?> myResponse;

  public HttpException(@NotNull Response<?> response) {
    super(EduCoreBundle.message("exception.message.http.info", response.code(), response.message()));
    myResponse = response;
  }

  @NotNull
  public Response<?> getResponse() {
    return myResponse;
  }
}
