package com.jetbrains.edu.learning.checkio.notifications.errors;

import com.jetbrains.edu.learning.checkio.notifications.CheckiONotification;
import org.jetbrains.annotations.NotNull;

public class CheckiOUnexpectedErrorNotification extends CheckiONotification.Error {
  private static final String CONTENT = "Unexpected error occurred";

  public CheckiOUnexpectedErrorNotification(@NotNull String title) {
    super(title, "", CONTENT, null);
  }
}
