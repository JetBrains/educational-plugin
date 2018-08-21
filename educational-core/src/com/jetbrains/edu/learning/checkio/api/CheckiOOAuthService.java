package com.jetbrains.edu.learning.checkio.api;

import com.intellij.openapi.diagnostic.Logger;
import com.jetbrains.edu.learning.checkio.account.CheckiOTokens;
import com.jetbrains.edu.learning.checkio.account.CheckiOUserInfo;
import org.jetbrains.annotations.NotNull;

import static com.jetbrains.edu.learning.checkio.api.RetrofitUtils.createRetrofitOAuthInterface;
import static com.jetbrains.edu.learning.checkio.api.RetrofitUtils.getResponse;

public final class CheckiOOAuthService {
  private static final Logger LOG = Logger.getInstance(CheckiOOAuthService.class);

  private CheckiOOAuthService() {}

  private static final CheckiOOAuthInterface RETROFIT_OAUTH_INTERFACE = createRetrofitOAuthInterface();

  private static void log(@NotNull String requestInfo) {
    LOG.info("Executing request: " + requestInfo);
  }

  @NotNull
  public static CheckiOResponse<CheckiOTokens> getTokens(
    @NotNull String grantType,
    @NotNull String clientSecret,
    @NotNull String clientId,
    @NotNull String code,
    @NotNull String redirectUri
  ) {
    log("get tokens");
    return getResponse(RETROFIT_OAUTH_INTERFACE.getTokens(grantType, clientSecret, clientId, code, redirectUri));
  }

  @NotNull
  public static CheckiOResponse<CheckiOTokens> refreshTokens(
    @NotNull String grantType,
    @NotNull String clientSecret,
    @NotNull String clientId,
    @NotNull String refreshToken
  ) {
    log("refresh tokens");
    return getResponse(RETROFIT_OAUTH_INTERFACE.refreshTokens(grantType, clientSecret, clientId, refreshToken));
  }

  @NotNull
  public static CheckiOResponse<CheckiOUserInfo> getUserInfo(@NotNull String accessToken) {
    log("get user info");
    return getResponse(RETROFIT_OAUTH_INTERFACE.getUserInfo(accessToken));
  }
}
