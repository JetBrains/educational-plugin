package com.jetbrains.edu.learning.checkio;

import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector;
import org.jetbrains.annotations.NotNull;

public interface CheckiOConnectorProvider {
  @NotNull
  CheckiOOAuthConnector getOAuthConnector();
}
