package com.jetbrains.edu.python.learning.checkio.connectors;

import com.intellij.util.Range;
import com.jetbrains.edu.learning.authUtils.CustomAuthorizationServer;
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector;
import com.jetbrains.edu.learning.checkio.utils.CheckiONames;
import com.jetbrains.edu.learning.checkio.utils.CheckiOOAuthBundle;
import com.jetbrains.edu.python.learning.checkio.PyCheckiOAccountHolder;
import org.jetbrains.annotations.NotNull;

public final class PyCheckiOOAuthConnector extends CheckiOOAuthConnector {
  private static String CLIENT_ID = CheckiOOAuthBundle.messageOrDefault("checkioClientId", "");
  private static String CLIENT_SECRET = CheckiOOAuthBundle.messageOrDefault("checkioClientSecret", "");

  private PyCheckiOOAuthConnector() {
    super(CLIENT_ID, CLIENT_SECRET, PyCheckiOAccountHolder.getInstance());
  }

  @Override
  protected String buildRedirectUri(int port) {
    return CheckiONames.CHECKIO_OAUTH_REDIRECT_HOST + ":" + port + CheckiONames.PY_CHECKIO_OAUTH_SERVICE_PATH;
  }

  @Override
  protected int createCustomServer() {
    return CustomAuthorizationServer.create(
      CheckiONames.PY_CHECKIO,
      new Range<>(36656, 36665),
      CheckiONames.PY_CHECKIO_OAUTH_SERVICE_PATH,
      this::afterCodeReceived
    );
  }

  @Override
  protected CustomAuthorizationServer getServerIfStarted() {
    return CustomAuthorizationServer.getServerIfStarted(CheckiONames.PY_CHECKIO);
  }

  private static class Holder {
    private static final PyCheckiOOAuthConnector INSTANCE = new PyCheckiOOAuthConnector();
  }

  @NotNull
  public static PyCheckiOOAuthConnector getInstance() {
    return Holder.INSTANCE;
  }
}
