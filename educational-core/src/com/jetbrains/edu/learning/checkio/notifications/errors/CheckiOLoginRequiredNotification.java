package com.jetbrains.edu.learning.checkio.notifications.errors;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.openapi.application.ApplicationManager;
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector;
import com.jetbrains.edu.learning.checkio.notifications.CheckiONotification;
import com.jetbrains.edu.learning.messages.EduCoreBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.HyperlinkEvent;

public class CheckiOLoginRequiredNotification extends CheckiONotification.Error {
  public CheckiOLoginRequiredNotification(@NotNull @Nls(capitalization = Nls.Capitalization.Sentence) String title,
                                          @NotNull CheckiOOAuthConnector oAuthConnector) {
    super(title, "", EduCoreBundle.message("notification.content.log.in.and.try.again.checkio"), new LoginLinkListener(oAuthConnector));
  }

  private static class LoginLinkListener extends NotificationListener.Adapter {
    private final CheckiOOAuthConnector myOAuthConnector;

    private LoginLinkListener(@NotNull CheckiOOAuthConnector oAuthConnector) {
      myOAuthConnector = oAuthConnector;
    }

    @Override
    protected void hyperlinkActivated(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
      myOAuthConnector.doAuthorize(() -> ApplicationManager.getApplication().invokeLater(
        () -> notification.hideBalloon()
      ));
    }
  }
}
