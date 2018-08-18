package com.jetbrains.edu.learning.checkio.notifications;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import icons.EducationalCoreIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.notification.NotificationDisplayType.BALLOON;
import static com.intellij.notification.NotificationDisplayType.STICKY_BALLOON;
import static com.intellij.notification.NotificationType.*;

public abstract class CheckiONotification extends Notification {
  private static final NotificationGroup CHECKIO_ERRORS = new NotificationGroup("CheckiO errors", STICKY_BALLOON, false);
  private static final NotificationGroup CHECKIO_WARNINGS = new NotificationGroup("CheckiO warnings", BALLOON, false);
  private static final NotificationGroup CHECKIO_INFOS = new NotificationGroup("CheckiO information", BALLOON, false);

  private CheckiONotification(
    @NotNull String groupDisplayId,
    @Nullable String title,
    @Nullable String subtitle,
    @Nullable String content,
    @NotNull NotificationType type,
    @Nullable NotificationListener listener
  ) {
    super(groupDisplayId, EducationalCoreIcons.CheckiO, title, subtitle, content, type, listener);
  }

  public static class Error extends CheckiONotification {
    public Error(
      @Nullable String title,
      @Nullable String subtitle,
      @Nullable String content,
      @Nullable NotificationListener listener
    ) {
      super(CHECKIO_ERRORS.getDisplayId(), title, subtitle, content, ERROR, listener);
    }
  }

  public static class Warning extends CheckiONotification {
    public Warning(
      @Nullable String title,
      @Nullable String subtitle,
      @Nullable String content,
      @Nullable NotificationListener listener
    ) {
      super(CHECKIO_WARNINGS.getDisplayId(), title, subtitle, content, WARNING, listener);
    }
  }

  public static class Info extends CheckiONotification {
    public Info(
      @Nullable String title,
      @Nullable String subtitle,
      @Nullable String content,
      @Nullable NotificationListener listener
    ) {
      super(CHECKIO_INFOS.getDisplayId(), title, subtitle, content, INFORMATION, listener);
    }
  }
}
