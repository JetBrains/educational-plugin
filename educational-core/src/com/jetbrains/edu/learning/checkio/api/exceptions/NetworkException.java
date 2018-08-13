package com.jetbrains.edu.learning.checkio.api.exceptions;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

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
