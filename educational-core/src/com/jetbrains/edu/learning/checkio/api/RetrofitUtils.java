package com.jetbrains.edu.learning.checkio.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.jetbrains.edu.learning.checkio.api.adapters.CheckiOMissionListDeserializer;
import com.jetbrains.edu.learning.checkio.call.CheckiOCallAdapterFactory;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission;
import com.jetbrains.edu.learning.checkio.utils.CheckiONames;
import org.jetbrains.annotations.NotNull;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.lang.reflect.Type;
import java.util.List;

public final class RetrofitUtils {
  private RetrofitUtils() {}

  public static CheckiOOAuthInterface createRetrofitOAuthInterface() {
    return createRetrofitInterface(CheckiONames.CHECKIO_OAUTH_HOST, new Gson(), CheckiOOAuthInterface.class);
  }

  public static CheckiOApiInterface createRetrofitApiInterface(@NotNull String apiBaseUrl) {
    return createRetrofitInterface(apiBaseUrl, createApiGson(), CheckiOApiInterface.class);
  }

  private static Gson createApiGson() {
    final GsonBuilder gsonBuilder = new GsonBuilder();
    final Type missionListType = new TypeToken<List<CheckiOMission>>() {}.getType();

    gsonBuilder.registerTypeAdapter(missionListType, new CheckiOMissionListDeserializer());

    return gsonBuilder.create();
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
