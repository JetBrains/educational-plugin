package com.jetbrains.edu.python.learning.checkio.utils;

import static com.jetbrains.edu.learning.checkio.utils.CheckiONames.CHECKIO_HOST;
import static com.jetbrains.edu.learning.checkio.utils.CheckiONames.CHECKIO_TEST_FORM_TARGET_PATH;

public final class PyCheckiONames {
  private PyCheckiONames() {}

  public static final String PY_CHECKIO_URL = "https://py." + CHECKIO_HOST;

  public static final String PY_CHECKIO_INTERPRETER = "python-3";

  public static final String PY_CHECKIO_TEST_FORM_TARGET_URL = PY_CHECKIO_URL + CHECKIO_TEST_FORM_TARGET_PATH + "/";
}
