package com.jetbrains.edu.learning.checkio.call;

import com.intellij.openapi.diagnostic.Logger;
import com.jetbrains.edu.learning.checkio.api.exceptions.ApiException;
import com.jetbrains.edu.learning.checkio.api.exceptions.HttpException;
import com.jetbrains.edu.learning.checkio.api.exceptions.NetworkException;
import com.jetbrains.edu.learning.checkio.api.exceptions.ParseException;
import org.jetbrains.annotations.NotNull;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;

public class CheckiOCall<T> {
  private static final Logger LOG = Logger.getInstance(CheckiOCall.class);

  private final Call<T> myOriginalCall;

  CheckiOCall(@NotNull Call<T> originalCall) {
    myOriginalCall = originalCall;
  }

  @NotNull
  public T execute() throws ApiException {
    LOG.info("Executing request: " + myOriginalCall.request().toString());

    try {
      final Response<T> response = myOriginalCall.execute();
      if (!response.isSuccessful()) {
        throw new HttpException(response);
      }

      final T body = response.body();
      if (body == null) {
        throw new ParseException(response.raw());
      }

      return body;
    }
    catch (IOException e) {
      throw new NetworkException(e);
    }
  }
}
