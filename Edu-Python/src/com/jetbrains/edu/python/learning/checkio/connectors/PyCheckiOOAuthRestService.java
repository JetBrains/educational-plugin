package com.jetbrains.edu.python.learning.checkio.connectors;

import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthRestService;
import com.jetbrains.edu.learning.checkio.utils.CheckiONames;
import org.jetbrains.annotations.NotNull;

public final class PyCheckiOOAuthRestService extends CheckiOOAuthRestService {
  private PyCheckiOOAuthRestService() {
    super(
      CheckiONames.PY_CHECKIO,
      CheckiONames.PY_CHECKIO_OAUTH_SERVICE_PATH,
      PyCheckiOOAuthConnector.getInstance()
    );
  }

  @NotNull
  @Override
  protected String getServiceName() {
    return CheckiONames.PY_CHECKIO_OAUTH_SERVICE_NAME;
  }
}
