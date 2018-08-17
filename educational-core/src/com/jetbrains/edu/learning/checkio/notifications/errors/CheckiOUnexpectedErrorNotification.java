package com.jetbrains.edu.learning.checkio.notifications.errors;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.jetbrains.edu.learning.checkio.notifications.CheckiONotificationGroups;
import org.jetbrains.annotations.NotNull;

public class CheckiOUnexpectedErrorNotification extends Notification {
  private static final NotificationGroup NOTIFICATION_GROUP = CheckiONotificationGroups.CHECKIO_ERRORS;
  private static final String DEFAULT_CONTENT = "Unexpected error occurred";

  public CheckiOUnexpectedErrorNotification(@NotNull String title) {
    this(title, DEFAULT_CONTENT);
  }

  public CheckiOUnexpectedErrorNotification(@NotNull String title, @NotNull String content) {
    super(
      NOTIFICATION_GROUP.getDisplayId(),
      title,
      content,
      NotificationType.ERROR
    );
  }
}
