package com.jetbrains.edu.learning.checkio.api.exceptions;

import org.jetbrains.annotations.NotNull;
import retrofit2.Response;

public class HttpException extends ApiException {
  public HttpException(@NotNull Response<?> response) {
    super("HTTP " + response.code() + " " + response.message());
  }
}
