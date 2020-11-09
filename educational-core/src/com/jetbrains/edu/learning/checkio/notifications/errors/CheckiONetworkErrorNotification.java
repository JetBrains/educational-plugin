package com.jetbrains.edu.learning.checkio.notifications.errors;

import com.jetbrains.edu.learning.checkio.notifications.CheckiONotification;
import com.jetbrains.edu.learning.messages.EduCoreBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

public class CheckiONetworkErrorNotification extends CheckiONotification.Warning {
  public CheckiONetworkErrorNotification(@Nullable @Nls(capitalization = Nls.Capitalization.Sentence) String title) {
    super(title, EduCoreBundle.message("notification.subtitle.connection.failed"),
          EduCoreBundle.message("notification.content.check.connection.and.try.again"), null);
  }
}
