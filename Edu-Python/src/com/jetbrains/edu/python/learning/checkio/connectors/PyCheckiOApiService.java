package com.jetbrains.edu.python.learning.checkio.connectors;

import com.jetbrains.edu.learning.checkio.api.CheckiOApiInterface;
import com.jetbrains.edu.learning.checkio.api.CheckiOApiService;
import com.jetbrains.edu.learning.checkio.utils.CheckiONames;

import static com.jetbrains.edu.learning.checkio.api.RetrofitUtils.createRetrofitInterface;

public final class PyCheckiOApiService extends CheckiOApiService {
  private PyCheckiOApiService() {
    super(createRetrofitInterface(CheckiONames.PY_CHECKIO_API_HOST, CheckiOApiInterface.class));
  }

  private static class Holder {
    private static final PyCheckiOApiService INSTANCE = new PyCheckiOApiService();
  }

  public static PyCheckiOApiService getInstance() {
    return Holder.INSTANCE;
  }
}
