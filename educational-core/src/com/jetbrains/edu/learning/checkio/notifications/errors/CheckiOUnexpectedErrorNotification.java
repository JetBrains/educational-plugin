package com.jetbrains.edu.learning.checkio.notifications.errors;

import com.jetbrains.edu.learning.checkio.notifications.CheckiONotification;
import com.jetbrains.edu.learning.messages.EduCoreBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

public class CheckiOUnexpectedErrorNotification extends CheckiONotification.Error {
  public CheckiOUnexpectedErrorNotification(@NotNull @Nls(capitalization = Nls.Capitalization.Sentence) String title) {
    super(title, "", EduCoreBundle.message("notification.content.unexpected.error.occurred"), null);
  }
}
