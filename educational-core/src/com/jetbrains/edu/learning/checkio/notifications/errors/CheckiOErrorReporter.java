package com.jetbrains.edu.learning.checkio.notifications.errors;

import com.intellij.notification.Notifications;
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector;
import org.jetbrains.annotations.NotNull;

public class CheckiOErrorReporter {
  private final String myTitle;

  public CheckiOErrorReporter(@NotNull String title) {
    myTitle = title;
  }

  public void reportLoginRequiredError(@NotNull CheckiOOAuthConnector oAuthConnector) {
    Notifications.Bus.notify(new CheckiOLoginRequiredNotification(myTitle, oAuthConnector));
  }

  public void reportNetworkError() {
    Notifications.Bus.notify(new CheckiONetworkErrorNotification(myTitle));
  }

  public void reportUnexpectedError() {
    Notifications.Bus.notify(new CheckiOUnexpectedErrorNotification(myTitle));
  }

  public void reportUnexpectedError(@NotNull String content) {
    Notifications.Bus.notify(new CheckiOUnexpectedErrorNotification(myTitle, content));
  }
}
