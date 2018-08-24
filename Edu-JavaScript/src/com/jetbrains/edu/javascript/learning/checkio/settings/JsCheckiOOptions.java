package com.jetbrains.edu.javascript.learning.checkio.settings;

import com.jetbrains.edu.javascript.learning.checkio.connectors.JsCheckiOOAuthConnector;
import com.jetbrains.edu.javascript.learning.checkio.utils.JsCheckiONames;
import com.jetbrains.edu.learning.checkio.options.CheckiOOptions;
import org.jetbrains.annotations.Nls;

public class JsCheckiOOptions extends CheckiOOptions {
  protected JsCheckiOOptions() {
    super(JsCheckiONames.JS_CHECKIO, JsCheckiOOAuthConnector.getInstance());
  }

  @Nls
  @Override
  public String getDisplayName() {
    return JsCheckiONames.JS_CHECKIO + " options";
  }
}
