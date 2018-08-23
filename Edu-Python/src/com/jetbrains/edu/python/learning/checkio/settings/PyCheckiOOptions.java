package com.jetbrains.edu.python.learning.checkio.settings;

import com.jetbrains.edu.learning.checkio.options.CheckiOOptions;
import com.jetbrains.edu.python.learning.checkio.connectors.PyCheckiOOAuthConnector;
import com.jetbrains.edu.python.learning.checkio.utils.PyCheckiONames;
import org.jetbrains.annotations.Nls;

public class PyCheckiOOptions extends CheckiOOptions {
  protected PyCheckiOOptions() {
    super(PyCheckiONames.PY_CHECKIO, PyCheckiOOAuthConnector.getInstance());
  }

  @Nls
  @Override
  public String getDisplayName() {
    return PyCheckiONames.PY_CHECKIO + " options";
  }
}
