package com.jetbrains.edu.python.learning.checkio.settings;

import com.jetbrains.edu.learning.checkio.options.CheckiOOptions;
import com.jetbrains.edu.learning.checkio.utils.CheckiONames;
import com.jetbrains.edu.python.learning.checkio.connectors.PyCheckiOOAuthConnector;
import com.jetbrains.edu.python.learning.checkio.utils.PyCheckiONames;
import org.jetbrains.annotations.Nls;

public class PyCheckiOOptions extends CheckiOOptions {
  protected PyCheckiOOptions() {
    super(PyCheckiOOAuthConnector.getInstance());
  }

  @Nls
  @Override
  public String getDisplayName() {
    return CheckiONames.PY_CHECKIO;
  }

  @Override
  protected String getApiHost() {
    return PyCheckiONames.PY_CHECKIO_API_HOST;
  }
}
