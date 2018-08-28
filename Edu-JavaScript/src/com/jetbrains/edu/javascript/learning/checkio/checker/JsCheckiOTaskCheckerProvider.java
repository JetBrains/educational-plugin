package com.jetbrains.edu.javascript.learning.checkio.checker;

import com.jetbrains.edu.javascript.learning.checkio.connectors.JsCheckiOApiConnector;
import com.jetbrains.edu.javascript.learning.checkio.utils.JsCheckiONames;
import com.jetbrains.edu.learning.checkio.checker.CheckiOTaskCheckerProvider;

public class JsCheckiOTaskCheckerProvider extends CheckiOTaskCheckerProvider {
  public JsCheckiOTaskCheckerProvider() {
    super(
      JsCheckiOApiConnector.getInstance(),
      JsCheckiONames.JS_CHECKIO_INTERPRETER,
      JsCheckiONames.JS_CHECKIO_TEST_FORM_TARGET_URL
    );
  }
}
