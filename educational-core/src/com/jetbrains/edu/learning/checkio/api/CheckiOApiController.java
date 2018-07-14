package com.jetbrains.edu.learning.checkio.api;

import com.jetbrains.edu.learning.checkio.CheckiONames;
import com.jetbrains.edu.learning.checkio.controllers.CheckiOAuthorizationController;
import com.jetbrains.edu.learning.checkio.model.CheckiOUser;
import com.jetbrains.edu.learning.checkio.model.Tokens;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.util.function.UnaryOperator;

public class CheckiOApiController {
  private CheckiOApiController() {}

  private static class CheckioApiControllerHolder {
    private static final CheckiOApiController INSTANCE = new CheckiOApiController();
  }

  public static CheckiOApiController getInstance() {
    return CheckioApiControllerHolder.INSTANCE;
  }

  private final Retrofit retrofit = new Retrofit.Builder()
    .baseUrl(CheckiONames.CHECKIO_URL)
    .addConverterFactory(GsonConverterFactory.create())
    .build();

  private final CheckiOApiService apiService = retrofit.create(CheckiOApiService.class);

  @Nullable
  public Tokens getTokens(@NotNull String code, @NotNull String redirectUri) {
    return getResponseBodyAndApply(
      apiService.getTokens(
        CheckiONames.GRANT_TYPE.AUTHORIZATION_CODE,
        CheckiONames.CLIENT_SECRET,
        CheckiONames.CLIENT_ID,
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
        CheckiONames.GRANT_TYPE.REFRESH_TOKEN,
        CheckiONames.CLIENT_SECRET,
        CheckiONames.CLIENT_ID,
        refreshToken
      ),
      (tokens) -> {
        tokens.received();
        return tokens;
      }
    );
  }

  @Nullable
  public CheckiOUser getUser(@NotNull Tokens tokens) {
    return getResponseBodyAndApply(
      apiService.getUserInfo(tokens.getAccessToken()),
      (user) -> {
        user.setTokens(tokens);
        return user;
      });
  }

  @Nullable
  public CheckiOUser getUser(@NotNull String code) {
    Tokens tokens = getTokens(code, CheckiOAuthorizationController.getOauthRedirectUri());
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
