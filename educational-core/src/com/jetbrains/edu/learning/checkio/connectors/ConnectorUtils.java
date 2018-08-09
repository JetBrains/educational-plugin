package com.jetbrains.edu.learning.checkio.connectors;

import com.intellij.openapi.diagnostic.Logger;
import com.jetbrains.edu.learning.checkio.api.wrappers.ResponseWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;

public final class ConnectorUtils {
  private static final Logger LOG = Logger.getInstance(ConnectorUtils.class);

  public static <T> T createRetrofitInterface(@NotNull String apiBaseUrl, @NotNull Class<T> apiServiceClass) {
    return new Retrofit.Builder()
      .baseUrl(apiBaseUrl)
      .addConverterFactory(GsonConverterFactory.create())
      .build()
      .create(apiServiceClass);
  }

  @Nullable
  public static <R, T extends ResponseWrapper<R>> R getResponseBodyAndUnwrap(@NotNull Call<T> call) {
    try {
      LOG.info("Executing request: " + call.request());

      final Response<T> response = call.execute();

      if (!response.isSuccessful()) {
        final String error = response.errorBody() == null ? "" : response.errorBody().string();
        LOG.warn("Unsuccessful response: " + response.code() + "," + error);
        return null;
      }

      T responseWrapperBody = response.body();

      if (responseWrapperBody == null) {
        LOG.warn("Response body is null: " + response.toString());
        return null;
      }

      return responseWrapperBody.unwrap();
    } catch (IOException e) {
      LOG.warn("Network error: " + e.getMessage());
      return null;
    }
  }
}
