package com.jetbrains.edu.python.learning.checkio.connectors;

import com.jetbrains.edu.learning.checkio.account.CheckiOAccount;
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector;
import com.jetbrains.edu.python.learning.checkio.PyCheckiOSettings;
import com.jetbrains.edu.python.learning.checkio.utils.PyCheckiONames;
import com.jetbrains.edu.python.learning.checkio.utils.PyCheckiOOAuthBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PyCheckiOOAuthConnector extends CheckiOOAuthConnector {
  private final static String CLIENT_ID = PyCheckiOOAuthBundle.messageOrDefault("pyCheckioClientId", "");
  private final static String CLIENT_SECRET = PyCheckiOOAuthBundle.messageOrDefault("pyCheckioClientSecret", "");

  private PyCheckiOOAuthConnector() {
    super(CLIENT_ID, CLIENT_SECRET);
  }

  @Nullable
  @Override
  public CheckiOAccount getAccount() {
    return PyCheckiOSettings.INSTANCE.getAccount();
  }

  @Override
  public void setAccount(@Nullable CheckiOAccount account) {
    PyCheckiOSettings.INSTANCE.setAccount(account);
  }

  @NotNull
  @Override
  public String getOAuthServicePath() {
    return PyCheckiONames.PY_CHECKIO_OAUTH_SERVICE_PATH;
  }

  @NotNull
  @Override
  protected String getPlatformName() {
    return PyCheckiONames.PY_CHECKIO;
  }

  private static class Holder {
    private static final PyCheckiOOAuthConnector INSTANCE = new PyCheckiOOAuthConnector();
  }

  @NotNull
  public static PyCheckiOOAuthConnector getInstance() {
    return Holder.INSTANCE;
  }
}
