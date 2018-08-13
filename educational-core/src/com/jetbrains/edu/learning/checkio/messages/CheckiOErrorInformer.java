package com.jetbrains.edu.learning.checkio.messages;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.Messages;
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class CheckiOErrorInformer {
  private static final String LOGIN_REQUIRED_MESSAGE = "Login required. Please, log in to CheckiO and try again.";
  private static final String LOGIN_REQUIRED_BUTTON_TEXT = "Log in";

  private static final String NETWORK_ERROR_MESSAGE = "Connection failed. Check your network connection and try again.";
  private static final String NETWORK_ERROR_BUTTON_TEXT = "Retry";

  private final CheckiOOAuthConnector myOAuthConnector;

  protected CheckiOErrorInformer(@NotNull CheckiOOAuthConnector oauthConnector) {
    myOAuthConnector = oauthConnector;
  }

  public void showLoginRequiredMessage(@NotNull String title) {
    int result = showOkCancelDialog(LOGIN_REQUIRED_MESSAGE, title, LOGIN_REQUIRED_BUTTON_TEXT);
    if (result == Messages.OK) {
      myOAuthConnector.doAuthorize();
    }
  }

  public int showNetworkErrorMessage(@NotNull String title) {
    return showOkCancelDialog(NETWORK_ERROR_MESSAGE, title, NETWORK_ERROR_BUTTON_TEXT);
  }

  private static int showOkCancelDialog(@NotNull String message, @NotNull String title, @NotNull String okText) {
    final AtomicInteger result = new AtomicInteger();

    ApplicationManager.getApplication().invokeAndWait(() -> result.set(
      Messages.showOkCancelDialog(
        message,
        title,
        okText,
        Messages.CANCEL_BUTTON,
        Messages.getWarningIcon()
      )
    ));

    return result.get();
  }

  public void showErrorDialog(@NotNull String message, @NotNull String title) {
    ApplicationManager.getApplication().invokeLater(() -> Messages.showErrorDialog(message, title));
  }
}
