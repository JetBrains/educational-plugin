package com.jetbrains.edu.learning.checkio.api;

import com.jetbrains.edu.learning.checkio.api.exceptions.ApiException;
import com.jetbrains.edu.learning.checkio.api.exceptions.HttpException;
import com.jetbrains.edu.learning.checkio.api.exceptions.NetworkException;
import com.jetbrains.edu.learning.checkio.api.exceptions.ParseException;
import com.jetbrains.edu.learning.checkio.api.wrappers.ResponseWrapper;
import org.jetbrains.annotations.NotNull;
import retrofit2.Response;

import java.io.IOException;

@FunctionalInterface
public interface MyResponse<T> {
  @NotNull
  T get() throws ApiException;

  static <T> MyResponse<T> createSuccessful(@NotNull ResponseWrapper<T> responseWrapper) {
    return () -> responseWrapper.unwrap();
  }

  static <T> MyResponse<T> createUnsuccessful(@NotNull Response<?> response) {
    return () -> {throw new HttpException(response);};
  }

  static <T> MyResponse<T> createNetworkError(@NotNull IOException e) {
    return () -> {throw new NetworkException(e);};
  }

  static <T> MyResponse<T> createParseError(@NotNull Response<?> response) {
    return () -> {throw new ParseException(response.raw());};
  }
}
