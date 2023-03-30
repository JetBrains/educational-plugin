package com.jetbrains.edu.javascript.learning.checkio.connectors;

import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthRestService;

import static com.jetbrains.edu.learning.checkio.utils.CheckiONames.JS_CHECKIO;

public class JsCheckiOOAuthRestService extends CheckiOOAuthRestService {
  protected JsCheckiOOAuthRestService() {
    super(JS_CHECKIO, JsCheckiOOAuthConnector.INSTANCE);
  }
}
