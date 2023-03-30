package com.jetbrains.edu.javascript.learning.checkio.utils;

import static com.jetbrains.edu.learning.checkio.utils.CheckiONames.CHECKIO_HOST;
import static com.jetbrains.edu.learning.checkio.utils.CheckiONames.CHECKIO_TEST_FORM_TARGET_PATH;

public final class JsCheckiONames {
  private JsCheckiONames() {}

  public static final String JS_CHECKIO_URL = "https://js." + CHECKIO_HOST;

  public static final String JS_CHECKIO_INTERPRETER = "js-node";

  public static final String JS_CHECKIO_TEST_FORM_TARGET_URL = JS_CHECKIO_URL + CHECKIO_TEST_FORM_TARGET_PATH + "/";
}
