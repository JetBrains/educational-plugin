package com.jetbrains.edu.javascript.learning.checkio.utils;

import static com.jetbrains.edu.learning.checkio.utils.CheckiONames.*;

public final class JsCheckiONames {
  private JsCheckiONames() {}

  public static final String JS_CHECKIO = "Js " + CHECKIO;

  public static final String JS_CHECKIO_URL = "js." + CHECKIO_URL;

  public static final String JS_CHECKIO_API_HOST = "https://" + JS_CHECKIO_URL;
  public static final String JS_CHECKIO_OAUTH_SERVICE_NAME = CHECKIO_OAUTH_SERVICE_NAME + "/js";
  public static final String JS_CHECKIO_OAUTH_SERVICE_PATH = CHECKIO_OAUTH_SERVICE_PATH + "/js";

  public static final String JS_CHECKIO_LANGUAGE = "CheckiO-JavaScript";
  public static final String JS_CHECKIO_INTERPRETER = "js-node";

  public static final String JS_CHECKIO_TEST_FORM_TARGET_URL = JS_CHECKIO_API_HOST + CHECKIO_TEST_FORM_TARGET_PATH + "/";
}
