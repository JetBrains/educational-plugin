package com.jetbrains.edu.python.learning.checkio.connectors;

import com.jetbrains.edu.learning.checkio.connectors.CheckiOApiConnector;
import com.jetbrains.edu.python.learning.checkio.utils.PyCheckiONames;
import org.jetbrains.annotations.NotNull;

import static com.jetbrains.edu.learning.checkio.api.RetrofitUtils.createRetrofitApiInterface;

public final class PyCheckiOApiConnector extends CheckiOApiConnector {
  private PyCheckiOApiConnector() {
    super(createRetrofitApiInterface(PyCheckiONames.PY_CHECKIO_API_HOST), PyCheckiOOAuthConnector.getInstance());
  }

  @NotNull
  @Override
  public String getLanguageId() {
    return "py";
  }

  private static class Holder {
    private static final PyCheckiOApiConnector INSTANCE = new PyCheckiOApiConnector();
  }

  public static PyCheckiOApiConnector getInstance() {
    return Holder.INSTANCE;
  }
}
