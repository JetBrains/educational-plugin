package com.jetbrains.edu.learning.checkio.notifications.errors;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector;
import com.jetbrains.edu.learning.checkio.notifications.CheckiONotificationGroups;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.HyperlinkEvent;

public class CheckiOLoginRequiredNotification extends Notification {
  private static final NotificationGroup NOTIFICATION_GROUP = CheckiONotificationGroups.CHECKIO_ERRORS;
  private static final String CONTENT = "Please, log in to CheckiO and try again.\n" +
                                        "<a href=\"#\">Log in</a>";

  public CheckiOLoginRequiredNotification(
    @NotNull String title,
    @NotNull CheckiOOAuthConnector oAuthConnector
  ) {
    super(
      NOTIFICATION_GROUP.getDisplayId(),
      title,
      CONTENT,
      NotificationType.ERROR,
      new LoginRequiredNotificationListener(oAuthConnector)
    );
  }

  private static class LoginRequiredNotificationListener extends NotificationListener.Adapter {
    private final CheckiOOAuthConnector myOAuthConnector;

    private LoginRequiredNotificationListener(@NotNull CheckiOOAuthConnector oAuthConnector) {
      myOAuthConnector = oAuthConnector;
    }

    @Override
    protected void hyperlinkActivated(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
      myOAuthConnector.doAuthorize(() -> notification.hideBalloon());
    }
  }
}
