package com.jetbrains.edu.learning.checkio.notifications.errors;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.jetbrains.edu.learning.checkio.notifications.CheckiONotificationGroups;
import org.jetbrains.annotations.Nullable;

public class CheckiONetworkErrorNotification extends Notification {
  private static final NotificationGroup NOTIFICATION_GROUP = CheckiONotificationGroups.CHECKIO_WARNINGS;
  private static final String SUBTITLE = "Connection failed";
  private static final String CONTENT = "Please, check your connection and try again";

  public CheckiONetworkErrorNotification(@Nullable String title) {
    super(
      NOTIFICATION_GROUP.getDisplayId(),
      null,
      title,
      SUBTITLE,
      CONTENT,
      NotificationType.WARNING,
      null
    );
  }
}
