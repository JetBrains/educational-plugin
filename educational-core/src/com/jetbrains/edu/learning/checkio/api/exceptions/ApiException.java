package com.jetbrains.edu.learning.checkio.api.exceptions;

import org.jetbrains.annotations.NotNull;

public abstract class ApiException extends Exception {
  public ApiException(@NotNull String message) {
    super(message);
  }

  public ApiException(@NotNull Throwable cause) {
    super(cause);
  }
}
