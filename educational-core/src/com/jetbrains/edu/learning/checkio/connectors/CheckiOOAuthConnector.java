package com.jetbrains.edu.learning.checkio.connectors;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.messages.Topic;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.authUtils.CustomAuthorizationServer;
import com.jetbrains.edu.learning.authUtils.OAuthUtils;
import com.jetbrains.edu.learning.checkio.account.CheckiOAccount;
import com.jetbrains.edu.learning.checkio.account.CheckiOTokens;
import com.jetbrains.edu.learning.checkio.account.CheckiOUserInfo;
import com.jetbrains.edu.learning.checkio.api.CheckiOOAuthService;
import com.jetbrains.edu.learning.checkio.api.exceptions.ApiException;
import com.jetbrains.edu.learning.checkio.api.exceptions.NetworkException;
import com.jetbrains.edu.learning.checkio.exceptions.CheckiOLoginRequiredException;
import com.jetbrains.edu.learning.checkio.utils.CheckiONames;
import org.apache.http.client.utils.URIBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.ide.BuiltInServerManager;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

public abstract class CheckiOOAuthConnector {
  private static final Logger LOG = Logger.getInstance(CheckiOOAuthConnector.class);

  private final String myClientId;
  private final String myClientSecret;
  private final Topic<CheckiOUserLoggedIn> myAuthorizationTopic = Topic.create("Edu.checkioUserLoggedIn", CheckiOUserLoggedIn.class);
  @NotNull private MessageBusConnection myAuthorizationBusConnection = ApplicationManager.getApplication().getMessageBus().connect();

  protected CheckiOOAuthConnector(@NotNull String clientId, @NotNull String clientSecret) {
    myClientId = clientId;
    myClientSecret = clientSecret;
  }

  @NotNull
  public abstract CheckiOAccount getAccount();

  public abstract void setAccount(@NotNull CheckiOAccount account);


  @NotNull
  public String getAccessToken() throws CheckiOLoginRequiredException, ApiException {
    requireUserLoggedIn();
    ensureTokensUpToDate();

    return getAccount().getTokens().getAccessToken();
  }

  @NotNull
  private CheckiOTokens getTokens(@NotNull String code, @NotNull String redirectUri) throws ApiException {
    requireClientPropertiesExist();

    return CheckiOOAuthService.getTokens(
      OAuthUtils.GRANT_TYPE.AUTHORIZATION_CODE,
      myClientSecret,
      myClientId,
      code,
      redirectUri
    ).get();
  }

  @SuppressWarnings("MethodMayBeStatic")
  @NotNull
  private CheckiOUserInfo getUserInfo(@NotNull String accessToken) throws ApiException {
    return CheckiOOAuthService.getUserInfo(accessToken).get();
  }

  @NotNull
  private CheckiOTokens refreshTokens(@NotNull String refreshToken) throws ApiException {
    requireClientPropertiesExist();

    return CheckiOOAuthService.refreshTokens(
      OAuthUtils.GRANT_TYPE.REFRESH_TOKEN,
      myClientSecret,
      myClientId,
      refreshToken
    ).get();
  }

  private void ensureTokensUpToDate() throws CheckiOLoginRequiredException, ApiException {
    requireUserLoggedIn();

    if (!getAccount().getTokens().isUpToDate()) {
      final String refreshToken = getAccount().getTokens().getRefreshToken();
      final CheckiOTokens newTokens = refreshTokens(refreshToken);
      getAccount().updateTokens(newTokens);
    }
  }

  private void requireClientPropertiesExist() {
    final Pattern spacesStringPattern = Pattern.compile("\\p{javaWhitespace}*");
    if (spacesStringPattern.matcher(myClientId).matches() || spacesStringPattern.matcher(myClientSecret).matches()) {
      final String errorMessage = "Client properties are not provided";
      LOG.error(errorMessage);
      throw new IllegalStateException(errorMessage);
    }
  }

  private void requireUserLoggedIn() throws CheckiOLoginRequiredException {
    if (!getAccount().isLoggedIn()) {
      throw new CheckiOLoginRequiredException();
    }
  }

  public void doAuthorize(@NotNull Runnable... postLoginActions) {
    requireClientPropertiesExist();

    try {
      final String handlerUri = getOAuthHandlerUri();
      final URI oauthLink = getOauthLink(handlerUri);

      createAuthorizationListener(postLoginActions);
      BrowserUtil.browse(oauthLink);
    }
    catch (URISyntaxException | IOException e) {
      // TODO: show message
    }
  }

  @NotNull
  private String getOAuthHandlerUri() throws IOException {
    if (EduUtils.isAndroidStudio()) {
      return getCustomServer().getHandlingUri();
    }
    else {
      final int port = BuiltInServerManager.getInstance().getPort();

      if (port < 63342 || port > 63362) {
        throw new IOException("No ports available");
      }

      return buildRedirectUri(port);
    }
  }

  @NotNull
  private URI getOauthLink(@NotNull String oauthRedirectUri) throws URISyntaxException {
    return new URIBuilder(CheckiONames.CHECKIO_OAUTH_URL + "/")
      .addParameter("redirect_uri", oauthRedirectUri)
      .addParameter("response_type", "code")
      .addParameter("client_id", myClientId)
      .build();
  }

  protected void createAuthorizationListener(@NotNull Runnable... postLoginActions) {
    myAuthorizationBusConnection.disconnect();
    myAuthorizationBusConnection = ApplicationManager.getApplication().getMessageBus().connect();
    myAuthorizationBusConnection.subscribe(myAuthorizationTopic, () -> {
      for (Runnable action : postLoginActions) {
        action.run();
      }
    });
  }

  @NotNull
  protected abstract String buildRedirectUri(int port);

  @NotNull
  protected abstract CustomAuthorizationServer getCustomServer() throws IOException;

  // In case of built-in server
  @Nullable
  public String codeHandler(@NotNull String code) {
    return codeHandler(code, buildRedirectUri(BuiltInServerManager.getInstance().getPort()));
  }

  // In case of Android Studio
  @Nullable
  public synchronized String codeHandler(@NotNull String code, @NotNull String handlingPath) {
    try {
      if (getAccount().isLoggedIn()) {
        ApplicationManager.getApplication().getMessageBus().syncPublisher(myAuthorizationTopic).userLoggedIn();
        return "You're logged in already";
      }

      final CheckiOTokens tokens = getTokens(code, handlingPath);
      final CheckiOUserInfo userInfo = getUserInfo(tokens.getAccessToken());
      getAccount().logIn(userInfo, tokens);
      ApplicationManager.getApplication().getMessageBus().syncPublisher(myAuthorizationTopic).userLoggedIn();
      return null;
    }
    catch (NetworkException e) {
      return "Connection failed";
    }
    catch (ApiException e) {
      return "Couldn't get user info";
    }
  }

  @FunctionalInterface
  private interface CheckiOUserLoggedIn {
    void userLoggedIn();
  }
}
