package com.jetbrains.edu.learning.checkio.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.jetbrains.edu.learning.checkio.api.adapters.CheckiOMissionListDeserializer;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission;
import com.jetbrains.edu.learning.checkio.utils.CheckiONames;
import org.jetbrains.annotations.NotNull;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

public final class RetrofitUtils {
  private RetrofitUtils() {}

  public static CheckiOApiInterface createRetrofitApiInterface(@NotNull String apiBaseUrl) {
    final Gson gson = createGson();
    return new Retrofit.Builder()
      .baseUrl(apiBaseUrl)
      .addConverterFactory(GsonConverterFactory.create(gson))
      .build()
      .create(CheckiOApiInterface.class);
  }

  private static Gson createGson() {
    final GsonBuilder gsonBuilder = new GsonBuilder();
    final Type missionListType = new TypeToken<List<CheckiOMission>>() {}.getType();

    gsonBuilder.registerTypeAdapter(missionListType, new CheckiOMissionListDeserializer());

    return gsonBuilder.create();
  }

  public static CheckiOOAuthInterface createRetrofitOAuthInterface() {
    return new Retrofit.Builder()
      .baseUrl(CheckiONames.CHECKIO_OAUTH_HOST)
      .addConverterFactory(GsonConverterFactory.create())
      .build()
      .create(CheckiOOAuthInterface.class);
  }

  @NotNull
  public static <T> CheckiOResponse<T> getResponse(@NotNull Call<T> call) {
    try {
      final Response<T> response = call.execute();
      if (!response.isSuccessful()) {
        return CheckiOResponse.createUnsuccessful(response);
      }

      final T responseWrapperBody = response.body();
      if (responseWrapperBody == null) {
        return CheckiOResponse.createParseError(response);
      }

      return CheckiOResponse.createSuccessful(responseWrapperBody);
    }
    catch (IOException e) {
      return CheckiOResponse.createNetworkError(e);
    }
  }
}
