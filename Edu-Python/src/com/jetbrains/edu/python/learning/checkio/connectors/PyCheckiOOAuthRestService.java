package com.jetbrains.edu.python.learning.checkio.connectors;

import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthRestService;

import static com.jetbrains.edu.learning.checkio.utils.CheckiONames.PY_CHECKIO;

public final class PyCheckiOOAuthRestService extends CheckiOOAuthRestService {
  private PyCheckiOOAuthRestService() {
    super(PY_CHECKIO, PyCheckiOOAuthConnector.INSTANCE);
  }
}
