package com.jetbrains.edu.learning.checkio.api.exceptions;

import com.jetbrains.edu.learning.checkio.call.CheckiOCall;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * This exception is thrown when network error occurred,
 * e.g. internet connection is disabled or endpoint doesn't respond
 *
 * @see CheckiOCall#execute()
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
