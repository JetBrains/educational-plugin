package com.jetbrains.edu.python.learning.checkio.settings;

import com.jetbrains.edu.learning.checkio.account.CheckiOAccount;
import com.jetbrains.edu.learning.checkio.options.CheckiOOptions;
import com.jetbrains.edu.learning.checkio.utils.CheckiONames;
import com.jetbrains.edu.python.learning.checkio.connectors.PyCheckiOOAuthConnector;
import com.jetbrains.edu.python.learning.checkio.utils.PyCheckiOUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

public class PyCheckiOOptions extends CheckiOOptions {
  protected PyCheckiOOptions() {
    super(PyCheckiOOAuthConnector.INSTANCE);
  }

  @Nls
  @Override
  public String getDisplayName() {
    return CheckiONames.PY_CHECKIO;
  }

  @NotNull
  @Override
  protected String profileUrl(@NotNull CheckiOAccount account) {
    return PyCheckiOUtils.getProfileUrl(account);
  }
}
