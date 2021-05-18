package com.jetbrains.edu.learning.checkio.notifications;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import icons.EducationalCoreIcons;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.notification.NotificationType.*;

public abstract class CheckiONotification extends Notification {

  private CheckiONotification(
    @NotNull String groupDisplayId,
    @Nullable @Nls(capitalization = Nls.Capitalization.Sentence) String title,
    @Nullable @Nls(capitalization = Nls.Capitalization.Sentence) String subtitle,
    @Nullable @Nls(capitalization = Nls.Capitalization.Sentence) String content,
    @NotNull NotificationType type,
    @Nullable NotificationListener listener
  ) {
    super(groupDisplayId, EducationalCoreIcons.CheckiO, title, subtitle, content, type, listener);
  }

  public static class Error extends CheckiONotification {
    public Error(
      @Nullable @Nls(capitalization = Nls.Capitalization.Sentence) String title,
      @Nullable @Nls(capitalization = Nls.Capitalization.Sentence) String subtitle,
      @Nullable @Nls(capitalization = Nls.Capitalization.Sentence) String content,
      @Nullable NotificationListener listener
    ) {
      super("EduTools", title, subtitle, content, ERROR, listener);
    }
  }

  public static class Warning extends CheckiONotification {
    public Warning(
      @Nullable @Nls(capitalization = Nls.Capitalization.Sentence) String title,
      @Nullable @Nls(capitalization = Nls.Capitalization.Sentence) String subtitle,
      @Nullable @Nls(capitalization = Nls.Capitalization.Sentence) String content,
      @Nullable NotificationListener listener
    ) {
      super("EduTools", title, subtitle, content, WARNING, listener);
    }
  }

  public static class Info extends CheckiONotification {
    public Info(
      @Nullable @Nls(capitalization = Nls.Capitalization.Sentence) String title,
      @Nullable @Nls(capitalization = Nls.Capitalization.Sentence) String subtitle,
      @Nullable @Nls(capitalization = Nls.Capitalization.Sentence) String content,
      @Nullable NotificationListener listener
    ) {
      super("EduTools", title, subtitle, content, INFORMATION, listener);
    }
  }
}
