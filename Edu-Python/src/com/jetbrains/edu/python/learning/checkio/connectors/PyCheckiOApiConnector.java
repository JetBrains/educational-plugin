package com.jetbrains.edu.python.learning.checkio.connectors;

import com.jetbrains.edu.learning.checkio.connectors.CheckiOApiConnector;

public final class PyCheckiOApiConnector extends CheckiOApiConnector {
  private PyCheckiOApiConnector() {
    super(PyCheckiOApiService.getInstance(), PyCheckiOOAuthConnector.getInstance());
  }

  private static class Holder {
    private static final PyCheckiOApiConnector INSTANCE = new PyCheckiOApiConnector();
  }

  public static PyCheckiOApiConnector getInstance() {
    return Holder.INSTANCE;
  }
}
