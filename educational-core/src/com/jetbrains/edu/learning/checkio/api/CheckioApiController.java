package com.jetbrains.edu.learning.checkio.api;

import com.jetbrains.edu.learning.checkio.CheckioNames;
import com.jetbrains.edu.learning.checkio.controllers.CheckioAuthorizationController;
import com.jetbrains.edu.learning.checkio.model.CheckioUser;
import com.jetbrains.edu.learning.checkio.model.Tokens;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.util.function.UnaryOperator;

public class CheckioApiController {
  private static class CheckioApiControllerHolder {
    public static final CheckioApiController INSTANCE = new CheckioApiController();
  }

  public static CheckioApiController getInstance() {
    return CheckioApiControllerHolder.INSTANCE;
  }

  private final Retrofit retrofit = new Retrofit.Builder()
    .baseUrl(CheckioNames.CHECKIO_URL)
    .addConverterFactory(GsonConverterFactory.create())
    .build();

  private final CheckioApiService apiService = retrofit.create(CheckioApiService.class);

  @Nullable
  public Tokens getTokens(@NotNull String code, @NotNull String redirectUri) {
    return getResponseBodyAndApply(
      apiService.getTokens(
        CheckioNames.GRANT_TYPE.AUTHORIZATION_CODE,
        CheckioNames.CLIENT_SECRET,
        CheckioNames.CLIENT_ID,
        code,
        redirectUri
      ),
      (tokens) -> {
        tokens.received();
        return tokens;
      }
    );
  }

  @Nullable
  public Tokens refreshTokens(@NotNull String refreshToken) {
    return getResponseBodyAndApply(
      apiService.refreshTokens(
        CheckioNames.GRANT_TYPE.REFRESH_TOKEN,
        CheckioNames.CLIENT_SECRET,
        CheckioNames.CLIENT_ID,
        refreshToken
      ),
      (tokens) -> {
        tokens.received();
        return tokens;
      }
    );
  }

  @Nullable
  public CheckioUser getUser(@NotNull Tokens tokens) {
    return getResponseBodyAndApply(
      apiService.getUserInfo(tokens.getAccessToken()),
      (user) -> {
        user.setTokens(tokens);
        return user;
      });
  }

  @Nullable
  public CheckioUser getUser(@NotNull String code) {
    Tokens tokens = getTokens(code, CheckioAuthorizationController.getOauthRedirectUri());
    return tokens == null ? null : getResponseBodyAndApply(
      apiService.getUserInfo(tokens.getAccessToken()),
      (user) -> {
        user.setTokens(tokens);
        return user;
      });
  }

  @Nullable
  private static <T> T getResponseBodyAndApply(Call<T> call, UnaryOperator<T> function) {
    try {
      Response<T> response = call.execute();
      if (response.isSuccessful()) {
        T responseBody = response.body();
        if (responseBody != null) {
          responseBody = function.apply(responseBody);
        }
        return responseBody;
      }
      return null;
    } catch (IOException e) {
      return null;
    }
  }
}
