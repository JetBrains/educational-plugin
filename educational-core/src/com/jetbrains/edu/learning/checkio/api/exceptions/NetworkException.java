package com.jetbrains.edu.learning.checkio.api.exceptions;

import com.jetbrains.edu.learning.checkio.api.CheckiOResponse;
import com.jetbrains.edu.learning.checkio.api.RetrofitUtils;
import org.jetbrains.annotations.NotNull;
import retrofit2.Call;

import java.io.IOException;

/**
 * This exception is thrown when network error occurred,
 * e.g. internet connection is disabled or endpoint doesn't respond
 *
 * @see CheckiOResponse#createNetworkError(IOException)
 * @see RetrofitUtils#getResponse(Call)
 * */
public class NetworkException extends ApiException {
  public NetworkException() {
    this("Connection failed");
  }

  public NetworkException(@NotNull String message) {
    super(message);
  }

  public NetworkException(@NotNull IOException e) {
    super(e);
  }
}
