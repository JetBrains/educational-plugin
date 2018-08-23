package com.jetbrains.edu.learning.checkio.notifications.errors;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector;
import com.jetbrains.edu.learning.checkio.notifications.CheckiONotification;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.HyperlinkEvent;

public class CheckiOLoginRequiredNotification extends CheckiONotification.Error {
  private static final String CONTENT = "Please, log in to CheckiO and try again.\n" +
                                        "<a href=\"#\">Log in</a>";

  public CheckiOLoginRequiredNotification(@NotNull String title, @NotNull CheckiOOAuthConnector oAuthConnector) {
    super(title, "", CONTENT, new LoginLinkListener(oAuthConnector));
  }

  private static class LoginLinkListener extends NotificationListener.Adapter {
    private final CheckiOOAuthConnector myOAuthConnector;

    private LoginLinkListener(@NotNull CheckiOOAuthConnector oAuthConnector) {
      myOAuthConnector = oAuthConnector;
    }

    @Override
    protected void hyperlinkActivated(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
      myOAuthConnector.doAuthorize(() -> notification.hideBalloon());
    }
  }
}
