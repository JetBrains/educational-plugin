package com.jetbrains.edu.javascript.learning.checkio.connectors;

import com.jetbrains.edu.javascript.learning.checkio.utils.JsCheckiONames;
import com.jetbrains.edu.learning.checkio.connectors.CheckiOApiConnector;

import static com.jetbrains.edu.learning.checkio.api.RetrofitUtils.createRetrofitApiInterface;

public class JsCheckiOApiConnector extends CheckiOApiConnector {
  protected JsCheckiOApiConnector() {
    super(createRetrofitApiInterface(JsCheckiONames.JS_CHECKIO_API_HOST), JsCheckiOOAuthConnector.getInstance());
  }

  private static class Holder {
    private static final JsCheckiOApiConnector INSTANCE = new JsCheckiOApiConnector();
  }

  public static JsCheckiOApiConnector getInstance() {
    return Holder.INSTANCE;
  }
}
