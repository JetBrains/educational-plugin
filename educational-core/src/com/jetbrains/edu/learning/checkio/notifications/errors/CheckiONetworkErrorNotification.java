package com.jetbrains.edu.learning.checkio.notifications.errors;

import com.jetbrains.edu.learning.checkio.notifications.CheckiONotification;
import org.jetbrains.annotations.Nullable;

public class CheckiONetworkErrorNotification extends CheckiONotification.Warning {
  private static final String SUBTITLE = "Connection failed";
  private static final String CONTENT = "Please, check your connection and try again";

  public CheckiONetworkErrorNotification(@Nullable String title) {
    super(title, SUBTITLE, CONTENT, null);
  }
}
