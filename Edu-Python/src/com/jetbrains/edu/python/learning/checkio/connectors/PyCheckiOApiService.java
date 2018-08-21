package com.jetbrains.edu.python.learning.checkio.connectors;

import com.jetbrains.edu.learning.checkio.api.CheckiOApiService;
import com.jetbrains.edu.python.learning.checkio.utils.PyCheckiONames;

import static com.jetbrains.edu.learning.checkio.api.RetrofitUtils.createRetrofitApiInterface;

public final class PyCheckiOApiService extends CheckiOApiService {
  private PyCheckiOApiService() {
    super(createRetrofitApiInterface(PyCheckiONames.PY_CHECKIO_API_HOST));
  }

  private static class Holder {
    private static final PyCheckiOApiService INSTANCE = new PyCheckiOApiService();
  }

  public static PyCheckiOApiService getInstance() {
    return Holder.INSTANCE;
  }
}
