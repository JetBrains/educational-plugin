package com.jetbrains.edu.learning.checkio.api;

import com.jetbrains.edu.learning.checkio.api.wrappers.ResponseWrapper;
import org.jetbrains.annotations.NotNull;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;

public final class RetrofitUtils {
  private RetrofitUtils() {}

  public static <T> T createRetrofitInterface(@NotNull String apiBaseUrl, @NotNull Class<T> apiServiceClass) {
    return new Retrofit.Builder()
      .baseUrl(apiBaseUrl)
      .addConverterFactory(GsonConverterFactory.create())
      .build()
      .create(apiServiceClass);
  }

  @NotNull
  public static <R, T extends ResponseWrapper<R>> MyResponse<R> getResponse(@NotNull Call<T> call) {
    try {
      final Response<T> response = call.execute();
      if (!response.isSuccessful()) {
        return MyResponse.createUnsuccessful(response);
      }

      final T responseWrapperBody = response.body();
      if (responseWrapperBody == null) {
        return MyResponse.createParseError(response);
      }

      return MyResponse.createSuccessful(responseWrapperBody);
    } catch (IOException e) {
      return MyResponse.createNetworkError(e);
    }
  }
}
