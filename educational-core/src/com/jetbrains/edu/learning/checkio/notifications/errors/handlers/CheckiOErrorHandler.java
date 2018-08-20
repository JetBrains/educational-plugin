package com.jetbrains.edu.learning.checkio.notifications.errors.handlers;

import com.intellij.openapi.diagnostic.Logger;
import com.jetbrains.edu.learning.checkio.api.exceptions.NetworkException;
import com.jetbrains.edu.learning.checkio.exceptions.CheckiOLoginRequiredException;
import org.jetbrains.annotations.NotNull;

public interface CheckiOErrorHandler {
  Logger LOG = Logger.getInstance(CheckiOErrorHandler.class);

  default void onLoginRequired() {}
  default void onNetworkError() {}
  default void onUnexpectedError() {}

  default void handle(@NotNull Exception e) {
    LOG.warn(e);
    if (e instanceof CheckiOLoginRequiredException) {
      onLoginRequired();
    } else if (e instanceof NetworkException) {
      onNetworkError();
    } else {
      onUnexpectedError();
    }
  }
}
