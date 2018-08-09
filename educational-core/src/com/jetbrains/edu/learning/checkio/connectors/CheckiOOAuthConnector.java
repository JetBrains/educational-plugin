package com.jetbrains.edu.learning.checkio.connectors;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.messages.Topic;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.authUtils.CustomAuthorizationServer;
import com.jetbrains.edu.learning.checkio.api.CheckiOOAuthService;
import com.jetbrains.edu.learning.checkio.model.CheckiOAccountHolder;
import com.jetbrains.edu.learning.checkio.model.CheckiOUserInfo;
import com.jetbrains.edu.learning.checkio.model.Tokens;
import com.jetbrains.edu.learning.checkio.utils.CheckiONames;
import org.apache.http.client.utils.URIBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.ide.BuiltInServerManager;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

public abstract class CheckiOOAuthConnector {
  private static final Logger LOG = Logger.getInstance(CheckiOOAuthConnector.class);

  private final String myClientId;
  private final String myClientSecret;
  private final CheckiOAccountHolder myAccountHolder;
  private final Topic<CheckiOUserLoggedIn> myAuthorizationTopic;
  private MessageBusConnection myAuthorizationBusConnection = ApplicationManager.getApplication().getMessageBus().connect();

  protected CheckiOOAuthConnector(
    @NotNull String clientId,
    @NotNull String clientSecret,
    @NotNull CheckiOAccountHolder accountHolder
  ) {
    myClientId = clientId;
    myClientSecret = clientSecret;
    myAccountHolder = accountHolder;
    myAuthorizationTopic = Topic.create("Edu.checkioUserLoggedIn", CheckiOUserLoggedIn.class);
  }

  @NotNull
  public CheckiOAccountHolder getAccountHolder() {
    return myAccountHolder;
  }

  @Nullable
  public String getAccessToken() {
    final Tokens currentTokens = requireTokensExistAndUpToDate();
    if (currentTokens == null) {
      return null;
    }

    return currentTokens.getAccessToken();
  }

  @Nullable
  public Tokens getTokens(@NotNull String code) {
    if (!requireClientPropertiesExist()) {
      return null;
    }

    final Tokens tokens = CheckiOOAuthService.getTokens(
      CheckiONames.GRANT_TYPE.AUTHORIZATION_CODE,
      myClientSecret,
      myClientId,
      code,
      getOauthRedirectUri()
    );
    myAccountHolder.getAccount().updateTokens(tokens);
    return tokens;
  }

  @Nullable
  public Tokens refreshTokens() {
    if (!requireClientPropertiesExist()) {
      return null;
    }

    final Tokens currentTokens = requireTokensExist();
    if (currentTokens == null) {
      return null;
    }

    final Tokens newTokens = CheckiOOAuthService.refreshTokens(
      CheckiONames.GRANT_TYPE.REFRESH_TOKEN,
      myClientSecret,
      myClientId,
      currentTokens.getRefreshToken()
    );

    myAccountHolder.getAccount().updateTokens(newTokens);
    return newTokens;
  }

  @Nullable
  public CheckiOUserInfo getUserInfo() {
    final Tokens currentTokens = requireTokensExistAndUpToDate();
    if (currentTokens == null) {
      return null;
    }

    return CheckiOOAuthService.getUserInfo(currentTokens.getAccessToken());
  }

  @Nullable
  private Tokens requireTokensExistAndUpToDate() {
    final Tokens currentTokens = requireTokensExist();
    if (currentTokens == null) {
      return null;
    } else if (!currentTokens.isUpToDate()) {
      final Tokens newTokens = refreshTokens();
      myAccountHolder.getAccount().updateTokens(newTokens);
      return newTokens;
    } else {
      return currentTokens;
    }
  }

  @Nullable
  private Tokens requireTokensExist() {
    final Tokens currentTokens = myAccountHolder.getAccount().getTokens();
    if (currentTokens == null) {
      LOG.warn("Tokens are not provided");
      return null;
    }

    return currentTokens;
  }

  public void doAuthorize(@NotNull Runnable... postLoginActions) {
    final URI oauthLink = getOauthLink();
    if (oauthLink == null) {
      return;
    }

    createAuthorizationListener(postLoginActions);

    BrowserUtil.browse(oauthLink);
  }

  protected void createAuthorizationListener(@NotNull Runnable... postLoginActions) {
    myAuthorizationBusConnection.disconnect();
    myAuthorizationBusConnection = ApplicationManager.getApplication().getMessageBus().connect();
    myAuthorizationBusConnection.subscribe(myAuthorizationTopic, () -> {
      for (Runnable action: postLoginActions) {
        action.run();
      }
    });
  }

  public String afterCodeReceived(@NotNull String code) {
    final Tokens newTokens = getTokens(code);
    final CheckiOUserInfo newUserInfo = getUserInfo();

    if (newUserInfo == null) {
      return "Couldn't get user info";
    } else {
      myAccountHolder.getAccount().logIn(newUserInfo, newTokens);
      ApplicationManager.getApplication().getMessageBus().syncPublisher(myAuthorizationTopic).userLoggedIn();
      return null;
    }
  }

  @Nullable
  private URI getOauthLink() {
    try {
      return new URIBuilder(CheckiONames.CHECKIO_OAUTH_URL + "/")
        .addParameter("redirect_uri", getOauthRedirectUri())
        .addParameter("response_type", "code")
        .addParameter("client_id", myClientId)
        .build();
    }
    catch (URISyntaxException e) {
      LOG.warn(e.getMessage());
      return null;
    }
  }

  @NotNull
  public String getOauthRedirectUri() {
    if (EduUtils.isAndroidStudio()) {
      final CustomAuthorizationServer startedServer = getServerIfStarted();

      if (startedServer != null) {
        return buildRedirectUri(startedServer.getPort());
      }

      int port = createCustomServer();

      if (port != -1) {
        return buildRedirectUri(port);
      }

      // TODO: show message
    }

    final int port = BuiltInServerManager.getInstance().getPort();
    return buildRedirectUri(port);
  }

  protected abstract String buildRedirectUri(int port);

  protected abstract int createCustomServer();

  protected abstract CustomAuthorizationServer getServerIfStarted();

  private boolean requireClientPropertiesExist() {
    final Pattern spacesStringPattern = Pattern.compile("\\p{javaWhitespace}*");

    if (spacesStringPattern.matcher(myClientId).matches()) {
      LOG.warn("client_id is not provided");
      return false;
    }
    if (spacesStringPattern.matcher(myClientSecret).matches()) {
      LOG.warn("client_secret is not provided");
      return false;
    }
    return true;
  }

  @FunctionalInterface
  protected interface CheckiOUserLoggedIn {
    void userLoggedIn();
  }
}
