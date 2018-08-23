package com.jetbrains.edu.learning.checkio.api;

import com.google.gson.Gson;
import com.jetbrains.edu.learning.checkio.call.CheckiOCallAdapterFactory;
import com.jetbrains.edu.learning.checkio.utils.CheckiONames;
import org.jetbrains.annotations.NotNull;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class RetrofitUtils {
  private RetrofitUtils() {}

  public static CheckiOOAuthInterface createRetrofitOAuthInterface() {
    return createRetrofitInterface(CheckiONames.CHECKIO_OAUTH_HOST, new Gson(), CheckiOOAuthInterface.class);
  }

  private static <T> T createRetrofitInterface(@NotNull String baseUrl, @NotNull Gson gson, @NotNull Class<T> apiInterfaceToken) {
    return new Retrofit.Builder()
      .addCallAdapterFactory(new CheckiOCallAdapterFactory())
      .addConverterFactory(GsonConverterFactory.create(gson))
      .baseUrl(baseUrl)
      .build()
      .create(apiInterfaceToken);
  }
}
