package com.jetbrains.edu.python.learning.checkio.checker;

import com.jetbrains.edu.learning.checkio.checker.CheckiOTaskCheckerProvider;
import com.jetbrains.edu.python.learning.checkio.connectors.PyCheckiOApiConnector;
import com.jetbrains.edu.python.learning.checkio.utils.PyCheckiONames;

public class PyCheckiOTaskCheckerProvider extends CheckiOTaskCheckerProvider {

  public PyCheckiOTaskCheckerProvider() {
    super(
      PyCheckiOApiConnector.getInstance(),
      PyCheckiONames.PY_CHECKIO_INTERPRETER,
      PyCheckiONames.PY_CHECKIO_TEST_FORM_TARGET_URL
    );
  }
}
