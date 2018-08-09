package com.jetbrains.edu.learning.checkio;

import com.jetbrains.edu.learning.EduConfigurator;
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector;
import org.jetbrains.annotations.NotNull;

public interface CheckiOConfigurator<Settings> extends EduConfigurator<Settings> {
  @NotNull
  CheckiOOAuthConnector getOAuthConnector();

  @NotNull
  @Override
  default String getTestFileName() {
    return "";
  }
}
