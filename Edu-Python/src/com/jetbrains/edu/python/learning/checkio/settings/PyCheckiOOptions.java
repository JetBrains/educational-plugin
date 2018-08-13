package com.jetbrains.edu.python.learning.checkio.settings;

import com.jetbrains.edu.learning.checkio.options.CheckiOOptions;
import com.jetbrains.edu.learning.checkio.utils.CheckiONames;
import com.jetbrains.edu.python.learning.checkio.connectors.PyCheckiOOAuthConnector;
import org.jetbrains.annotations.Nls;

public class PyCheckiOOptions extends CheckiOOptions {
  protected PyCheckiOOptions() {
    super(CheckiONames.PY_CHECKIO, PyCheckiOOAuthConnector.getInstance());
  }

  @Nls
  @Override
  public String getDisplayName() {
    return "Py CheckiO options";
  }
}
