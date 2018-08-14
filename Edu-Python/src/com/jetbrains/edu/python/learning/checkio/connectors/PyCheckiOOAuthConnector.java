package com.jetbrains.edu.python.learning.checkio.connectors;

import com.jetbrains.edu.learning.authUtils.CustomAuthorizationServer;
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector;
import com.jetbrains.edu.learning.checkio.model.CheckiOAccountHolder;
import com.jetbrains.edu.learning.checkio.utils.CheckiONames;
import com.jetbrains.edu.python.learning.checkio.PyCheckiOAccountHolder;
import com.jetbrains.edu.python.learning.checkio.utils.PyCheckiONames;
import com.jetbrains.edu.python.learning.checkio.utils.PyCheckiOOAuthBundle;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public final class PyCheckiOOAuthConnector extends CheckiOOAuthConnector {
  private static String CLIENT_ID = PyCheckiOOAuthBundle.messageOrDefault("checkioClientId", "");
  private static String CLIENT_SECRET = PyCheckiOOAuthBundle.messageOrDefault("checkioClientSecret", "");

  private PyCheckiOOAuthConnector() {
    super(CLIENT_ID, CLIENT_SECRET);
  }

  @NotNull
  @Override
  public CheckiOAccountHolder getAccountHolder() {
    return PyCheckiOAccountHolder.getInstance();
  }

  @NotNull
  @Override
  protected String buildRedirectUri(int port) {
    return CheckiONames.CHECKIO_OAUTH_REDIRECT_HOST + ":" + port + PyCheckiONames.PY_CHECKIO_OAUTH_SERVICE_PATH;
  }

  @NotNull
  protected CustomAuthorizationServer getCustomServer() throws IOException {
    final CustomAuthorizationServer startedServer =
      CustomAuthorizationServer.getServerIfStarted(PyCheckiONames.PY_CHECKIO);

    if (startedServer != null) {
      return startedServer;
    }

    return createCustomServer();
  }

  @NotNull
  private CustomAuthorizationServer createCustomServer() throws IOException {
    return CustomAuthorizationServer.create(
      PyCheckiONames.PY_CHECKIO,
      PyCheckiONames.PY_CHECKIO_OAUTH_SERVICE_PATH,
      this::afterCodeReceived
    );
  }

  private static class Holder {
    private static final PyCheckiOOAuthConnector INSTANCE = new PyCheckiOOAuthConnector();
  }

  @NotNull
  public static PyCheckiOOAuthConnector getInstance() {
    return Holder.INSTANCE;
  }
}
