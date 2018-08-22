package com.jetbrains.edu.learning.checkio.notifications.errors.handlers;

import com.intellij.openapi.diagnostic.Logger;
import com.jetbrains.edu.learning.checkio.api.exceptions.NetworkException;
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector;
import com.jetbrains.edu.learning.checkio.exceptions.CheckiOLoginRequiredException;
import com.jetbrains.edu.learning.checkio.notifications.errors.CheckiOErrorReporter;
import org.jetbrains.annotations.NotNull;

public class CheckiOErrorHandler {
  private static final Logger LOG = Logger.getInstance(CheckiOErrorHandler.class);

  private final CheckiOErrorReporter myErrorReporter;
  private final CheckiOOAuthConnector myOAuthConnector;

  public CheckiOErrorHandler(@NotNull String title, @NotNull CheckiOOAuthConnector oAuthConnector) {
    myErrorReporter = new CheckiOErrorReporter(title);
    myOAuthConnector = oAuthConnector;
  }

  public void onLoginRequired() {
    myErrorReporter.reportLoginRequiredError(myOAuthConnector);
  }

  public void onNetworkError() {
    myErrorReporter.reportNetworkError();
  }

  public void onUnexpectedError() {
    myErrorReporter.reportUnexpectedError();
  }

  public void handle(@NotNull Exception e) {
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
