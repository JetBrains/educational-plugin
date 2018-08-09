package com.jetbrains.edu.python.learning.checkio.connectors;

import com.jetbrains.edu.learning.checkio.api.CheckiOApiInterface;
import com.jetbrains.edu.learning.checkio.api.CheckiOApiService;
import com.jetbrains.edu.learning.checkio.connectors.ConnectorUtils;
import com.jetbrains.edu.learning.checkio.utils.CheckiONames;

public final class PyCheckiOApiService extends CheckiOApiService {
  private PyCheckiOApiService() {
    super(ConnectorUtils.createRetrofitInterface(CheckiONames.PY_CHECKIO_API_HOST, CheckiOApiInterface.class));
  }

  private static class Holder {
    private static final PyCheckiOApiService INSTANCE = new PyCheckiOApiService();
  }

  public static PyCheckiOApiService getInstance() {
    return Holder.INSTANCE;
  }
}
