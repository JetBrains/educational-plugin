package com.jetbrains.edu.javascript.learning.checkio.connectors;

import com.jetbrains.edu.javascript.learning.checkio.JsCheckiOSettings;
import com.jetbrains.edu.javascript.learning.checkio.utils.JsCheckiONames;
import com.jetbrains.edu.javascript.learning.checkio.utils.JsCheckiOOAuthBundle;
import com.jetbrains.edu.learning.checkio.account.CheckiOAccount;
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JsCheckiOOAuthConnector extends CheckiOOAuthConnector {
  private static final String CLIENT_ID = JsCheckiOOAuthBundle.messageOrDefault("jsCheckioClientId", "");
  private static final String CLIENT_SECRET = JsCheckiOOAuthBundle.messageOrDefault("jsCheckioClientSecret", "");

  protected JsCheckiOOAuthConnector() {
    super(CLIENT_ID, CLIENT_SECRET);
  }

  @Nullable
  @Override
  public CheckiOAccount getAccount() {
    return JsCheckiOSettings.getInstance().getAccount();
  }

  @Override
  public void setAccount(@Nullable CheckiOAccount account) {
    JsCheckiOSettings.getInstance().setAccount(account);
  }

  @NotNull
  @Override
  protected String getOAuthServicePath() {
    return JsCheckiONames.JS_CHECKIO_OAUTH_SERVICE_PATH;
  }

  @NotNull
  @Override
  protected String getPlatformName() {
    return JsCheckiONames.JS_CHECKIO;
  }


  private static class Holder {
    private static final JsCheckiOOAuthConnector INSTANCE = new JsCheckiOOAuthConnector();
  }

  public static JsCheckiOOAuthConnector getInstance() {
    return Holder.INSTANCE;
  }
}
