package com.jetbrains.edu.python.learning.checkio.connectors;

import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthRestService;
import com.jetbrains.edu.python.learning.checkio.utils.PyCheckiONames;
import org.jetbrains.annotations.NotNull;

public final class PyCheckiOOAuthRestService extends CheckiOOAuthRestService {
  private PyCheckiOOAuthRestService() {
    super(
      PyCheckiONames.PY_CHECKIO,
      PyCheckiONames.PY_CHECKIO_OAUTH_SERVICE_PATH,
      PyCheckiOOAuthConnector.getInstance()
    );
  }

  @NotNull
  @Override
  protected String getServiceName() {
    return PyCheckiONames.PY_CHECKIO_OAUTH_SERVICE_NAME;
  }
}
