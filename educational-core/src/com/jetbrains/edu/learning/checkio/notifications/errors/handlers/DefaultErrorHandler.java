package com.jetbrains.edu.learning.checkio.notifications.errors.handlers;

import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector;
import com.jetbrains.edu.learning.checkio.notifications.errors.CheckiOErrorReporter;
import org.jetbrains.annotations.NotNull;

public class DefaultErrorHandler implements CheckiOErrorHandler {
  private final CheckiOErrorReporter myErrorReporter;
  private final CheckiOOAuthConnector myOAuthConnector;

  public DefaultErrorHandler(@NotNull String title, @NotNull CheckiOOAuthConnector oAuthConnector) {
    myErrorReporter = new CheckiOErrorReporter(title);
    myOAuthConnector = oAuthConnector;
  }

  @Override
  public void onLoginRequired() {
    myErrorReporter.reportLoginRequiredError(myOAuthConnector);
  }

  @Override
  public void onNetworkError() {
    myErrorReporter.reportNetworkError();
  }

  @Override
  public void onUnexpectedError() {
    myErrorReporter.reportUnexpectedError();
  }
}
