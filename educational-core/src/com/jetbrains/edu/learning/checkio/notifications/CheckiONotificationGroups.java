package com.jetbrains.edu.learning.checkio.notifications;

import com.intellij.notification.NotificationGroup;

import static com.intellij.notification.NotificationDisplayType.BALLOON;
import static com.intellij.notification.NotificationDisplayType.STICKY_BALLOON;

public final class CheckiONotificationGroups {
  private CheckiONotificationGroups() {}

  public static final NotificationGroup CHECKIO_ERRORS = new NotificationGroup("CheckiO errors", STICKY_BALLOON, true);
  public static final NotificationGroup CHECKIO_WARNINGS = new NotificationGroup("CheckiO warnings", BALLOON, true);
}
