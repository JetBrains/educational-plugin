package com.jetbrains.edu.learning.checkio.api;

import com.jetbrains.edu.learning.checkio.connectors.ConnectorUtils;
import com.jetbrains.edu.learning.checkio.model.CheckiOUserInfo;
import com.jetbrains.edu.learning.checkio.model.Tokens;
import com.jetbrains.edu.learning.checkio.utils.CheckiONames;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CheckiOOAuthService {
  private CheckiOOAuthService() {}

  private static final CheckiOOAuthInterface myOAuthInterface =
    ConnectorUtils.createRetrofitInterface(CheckiONames.CHECKIO_OAUTH_HOST, CheckiOOAuthInterface.class);

  @Nullable
  public static Tokens getTokens(
    @NotNull String grantType,
    @NotNull String clientSecret,
    @NotNull String clientId,
    @NotNull String code,
    @NotNull String redirectUri
  ) {
    return ConnectorUtils.getResponseBodyAndUnwrap(myOAuthInterface.getTokens(grantType, clientSecret, clientId, code, redirectUri));
  }

  @Nullable
  public static Tokens refreshTokens(
    @NotNull String grantType,
    @NotNull String clientSecret,
    @NotNull String clientId,
    @NotNull String refreshToken
  ) {
    return ConnectorUtils.getResponseBodyAndUnwrap(myOAuthInterface.refreshTokens(grantType, clientSecret, clientId, refreshToken));
  }

  @Nullable
  public static CheckiOUserInfo getUserInfo(@NotNull String accessToken) {
    return ConnectorUtils.getResponseBodyAndUnwrap(myOAuthInterface.getUserInfo(accessToken));
  }
}
