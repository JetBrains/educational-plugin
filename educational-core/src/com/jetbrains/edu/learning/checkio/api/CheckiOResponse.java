package com.jetbrains.edu.learning.checkio.api;

import com.jetbrains.edu.learning.checkio.api.exceptions.ApiException;
import com.jetbrains.edu.learning.checkio.api.exceptions.HttpException;
import com.jetbrains.edu.learning.checkio.api.exceptions.NetworkException;
import com.jetbrains.edu.learning.checkio.api.exceptions.ParseException;
import org.jetbrains.annotations.NotNull;
import retrofit2.Response;

import java.io.IOException;

@FunctionalInterface
public interface CheckiOResponse<T> {
  @NotNull
  T get() throws ApiException;

  static <T> CheckiOResponse<T> createSuccessful(@NotNull T responseBody) {
    return () -> responseBody;
  }

  static <T> CheckiOResponse<T> createUnsuccessful(@NotNull Response<?> response) {
    return () -> {throw new HttpException(response);};
  }

  static <T> CheckiOResponse<T> createNetworkError(@NotNull IOException e) {
    return () -> {throw new NetworkException(e);};
  }

  static <T> CheckiOResponse<T> createParseError(@NotNull Response<?> response) {
    return () -> {throw new ParseException(response.raw());};
  }
}
