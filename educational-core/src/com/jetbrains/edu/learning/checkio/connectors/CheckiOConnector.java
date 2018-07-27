package com.jetbrains.edu.learning.checkio.connectors;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.messages.Topic;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.authUtils.CustomAuthorizationServer;
import com.jetbrains.edu.learning.checkio.CheckiONames;
import com.jetbrains.edu.learning.checkio.CheckiOOAuthBundle;
import com.jetbrains.edu.learning.checkio.api.CheckiOApiService;
import com.jetbrains.edu.learning.checkio.model.CheckiOMissionList;
import com.jetbrains.edu.learning.checkio.model.CheckiOUser;
import com.jetbrains.edu.learning.checkio.model.Tokens;
import com.jetbrains.edu.learning.checkio.settings.CheckiOSettings;
import org.apache.http.client.utils.URIBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.ide.BuiltInServerManager;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public final class CheckiOConnector {
  private CheckiOConnector() {}

  private static final Logger LOG = Logger.getInstance(CheckiOConnector.class);
  public static final Topic<CheckiOUserLoggedIn> LOGGED_IN = Topic.create("Edu.checkioUserLoggedIn", CheckiOUserLoggedIn.class);

  private static final String CLIENT_ID = CheckiOOAuthBundle.messageOrDefault("checkioClientId", "");
  private static final String CLIENT_SECRET = CheckiOOAuthBundle.messageOrDefault("checkioClientSecret", "");

  private static final CheckiOApiService API_SERVICE = new Retrofit.Builder()
    .baseUrl(CheckiONames.CHECKIO_URL)
    .addConverterFactory(GsonConverterFactory.create())
    .build()
    .create(CheckiOApiService.class);

  @Nullable
  public static String getAccessToken() {
    final Tokens currentTokens = requireTokensExistAndUpToDate();
    if (currentTokens == null) {
      return null;
    }
    return currentTokens.getAccessToken();
  }

   // In case of Android Studio redirect_uri passes directly
  @Nullable
  public static Tokens getTokens(@NotNull String code, @NotNull String redirectUri) {
    if (!requireClientPropertiesExist()) {
      return null;
    }

    return getResponseBodyAndApply(
      API_SERVICE.getTokens(
        CheckiONames.GRANT_TYPE.AUTHORIZATION_CODE,
        CLIENT_SECRET,
        CLIENT_ID,
        code,
        redirectUri
      ),
      (tokens) -> {
        tokens.markAsReceived();
        CheckiOSettings.getInstance().setTokens(tokens);
        return tokens;
      }
    );
  }

  @Nullable
  public static Tokens getTokens(@NotNull String code) {
    if (!requireClientPropertiesExist()) {
      return null;
    }

    return getResponseBodyAndApply(
      API_SERVICE.getTokens(
        CheckiONames.GRANT_TYPE.AUTHORIZATION_CODE,
        CLIENT_SECRET,
        CLIENT_ID,
        code,
        getOauthRedirectUri()
      ),
      (tokens) -> {
        tokens.markAsReceived();
        CheckiOSettings.getInstance().setTokens(tokens);
        return tokens;
      }
    );
  }

  @Nullable
  public static Tokens refreshTokens() {
    if (!requireClientPropertiesExist()) {
      return null;
    }

    final Tokens currentTokens = requireTokensExist();
    if (currentTokens == null) {
      return null;
    }

    return getResponseBodyAndApply(
      API_SERVICE.refreshTokens(
        CheckiONames.GRANT_TYPE.REFRESH_TOKEN,
        CLIENT_SECRET,
        CLIENT_ID,
        currentTokens.getRefreshToken()
      ),
      (tokens) -> {
        tokens.markAsReceived();
        CheckiOSettings.getInstance().setTokens(tokens);
        return tokens;
      }
    );
  }

  @Nullable
  public static CheckiOMissionList getMissionList() {
    final Tokens currentTokens = requireTokensExistAndUpToDate();
    if (currentTokens == null) {
      return null;
    }

    return getResponseBody(API_SERVICE.getMissionList(currentTokens.getAccessToken()));
  }

  @Nullable
  public static CheckiOUser getUser() {
    final Tokens currentTokens = requireTokensExistAndUpToDate();
    if (currentTokens == null) {
      return null;
    }

    return getResponseBodyAndApply(
      API_SERVICE.getUserInfo(currentTokens.getAccessToken()),
      (user) -> {
        CheckiOSettings.getInstance().setUser(user);
        return user;
      }
    );
  }

  @Nullable
  private static <T> T getResponseBodyAndApply(@NotNull Call<T> call, @NotNull UnaryOperator<T> function) {
    try {
      LOG.info("Executing request: " + call.request());

      final Response<T> response = call.execute();

      if (!response.isSuccessful()) {
        final String error = response.errorBody() == null ? "" : response.errorBody().string();
        LOG.error("Unsuccessful response: " + error);
        return null;
      }

      T responseBody = response.body();

      if (responseBody == null) {
        LOG.error("Response body is null: " + response.toString());
        return null;
      }

      return function.apply(responseBody);
    } catch (IOException e) {
      LOG.error(e);
      return null;
    }
  }

  @Nullable
  private static <T> T getResponseBody(@NotNull Call<T> call) {
    return getResponseBodyAndApply(call, UnaryOperator.identity());
  }

  private static boolean requireClientPropertiesExist() {
    final Pattern spacesStringPattern = Pattern.compile("\\p{javaWhitespace}*");

    if (spacesStringPattern.matcher(CLIENT_ID).matches()) {
      LOG.error("client_id is not provided");
      return false;
    }
    if (spacesStringPattern.matcher(CLIENT_SECRET).matches()) {
      LOG.error("client_secret is not provided");
      return false;
    }
    return true;
  }

  @Nullable
  private static Tokens requireTokensExistAndUpToDate() {
    final Tokens currentTokens = requireTokensExist();
    if (currentTokens == null) {
      return null;
    } else if (!currentTokens.isUpToDate()) {
      if (currentTokens.getRefreshToken() == null) {
        LOG.warn("Couldn't refresh tokens because `refresh token` is not provided");
        return null;
      } else {
        final Tokens newTokens = refreshTokens();
        CheckiOSettings.getInstance().setTokens(newTokens);
        return newTokens;
      }
    } else {
      return currentTokens;
    }
  }

  @Nullable
  private static Tokens requireTokensExist() {
    final Tokens currentTokens = CheckiOSettings.getInstance().getTokens();
    if (currentTokens == null) {
      LOG.warn("Tokens are not provided");
      return null;
    }

    if (currentTokens.getAccessToken() == null) {
      LOG.warn("Tokens exist but access token is not provided");
      return null;
    }

    if (currentTokens.getRefreshToken() == null) {
      LOG.warn("Tokens exist, but refresh token is not provided");
      return null;
    }

    return currentTokens;
  }

  public static void doAuthorize() {
    final URI oauthLink = getOauthLink();
    if (oauthLink == null) {
      return;
    }

    BrowserUtil.browse(oauthLink);
  }

  @Nullable
  private static URI getOauthLink() {
    try {
      return new URIBuilder(CheckiONames.CHECKIO_OAUTH_URL + "/")
        .addParameter("redirect_uri", getOauthRedirectUri())
        .addParameter("response_type", "code")
        .addParameter("client_id", CLIENT_ID)
        .build();
    }
    catch (URISyntaxException e) {
      LOG.error(e);
      return null;
    }
  }

  @NotNull
  public static String getOauthRedirectUri() {
    if (EduUtils.isAndroidStudio()) {
      CustomAuthorizationServer customServer = new CustomAuthorizationServer(CheckiONames.CHECKIO);

      int port = customServer.handle(createContextHandler(customServer));
      if (port != -1) {
        return "http://localhost:" + port;
      }
    }
    final int port = BuiltInServerManager.getInstance().getPort();
    return CheckiONames.EDU_CHECKIO_OAUTH_HOST + ":" + port + CheckiONames.EDU_CHECKIO_OAUTH_SERVICE;
  }

  @NotNull
  private static CustomAuthorizationServer.ContextHandler createContextHandler(@NotNull CustomAuthorizationServer server) {
    return new CustomAuthorizationServer.ContextHandler(server) {
      @Override
      public String afterCodeReceived(@NotNull String code) {
        final Tokens newTokens = getTokens(code, "http://localhost:" + getPort());
        final CheckiOUser newUser = getUser();

        if (newUser != null) {
          ApplicationManager.getApplication().getMessageBus().syncPublisher(LOGGED_IN).loggedIn(newTokens, newUser);
          return null;
        }

        return "Couldn't get user info";
      }
    };
  }

  @FunctionalInterface
  public interface CheckiOUserLoggedIn {
    void loggedIn(Tokens newTokens, CheckiOUser newUser);
  }
}
