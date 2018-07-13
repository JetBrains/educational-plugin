package com.jetbrains.edu.learning.checkio.controllers;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.util.messages.Topic;
import com.jetbrains.edu.learning.checkio.CheckiONames;
import com.jetbrains.edu.learning.checkio.api.CheckiOApiController;
import com.jetbrains.edu.learning.checkio.model.CheckiOUser;
import org.apache.http.client.utils.URIBuilder;
import org.jetbrains.annotations.NotNull;
import spark.Spark;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;

public class CheckiOAuthorizationController {
  private static String OAUTH_REDIRECT_HOST = "http://localhost";
  private static int OAUTH_REDIRECT_PORT = 36655;
  private static String OAUTH_REDIRECT_URI = OAUTH_REDIRECT_HOST + ":" + OAUTH_REDIRECT_PORT;

  public static final Topic<CheckioUserLoggedIn> LOGGED_IN = Topic.create("Edu.logInCheckiOUSer", CheckioUserLoggedIn.class);

  public static void doAuthorize() {
    startServer();
    URI uri = getOauthLink();
    BrowserUtil.browse(uri);
  }

  private static URI getOauthLink() {
    try {
      return new URIBuilder(CheckiONames.CHECKIO_OAUTH_URL + "/")
        .addParameter("redirect_uri", getOauthRedirectUri())
        .addParameter("response_type", "code")
        .addParameter("client_id", CheckiONames.CLIENT_ID)
        .build();
    }
    catch (URISyntaxException ignored) {
      return null;
    }
  }

  public static String getOauthRedirectUri() {
    return OAUTH_REDIRECT_URI;
  }

  private static String getOauthResultPage(boolean succeeded) {
    return succeeded
           ? getPageOrDefault(CheckiONames.CHECKIO_OAUTH_SUCCEED_PAGE, CheckiONames.CHECKIO_OAUTH_SUCCEED_DEFAULT_MESSAGE)
           : getPageOrDefault(CheckiONames.CHECKIO_OAUTH_FAILED_PAGE, CheckiONames.CHECKIO_OAUTH_FAILED_DEFAULT_MESSAGE);
  }

  private static String getPageOrDefault(@NotNull String pagePath, @NotNull String defaultMessage) {
    try (InputStream pageTemplateStream = CheckiOAuthorizationController.class.getResourceAsStream(pagePath)) {
      String pageTemplate = StreamUtil.readText(pageTemplateStream, Charset.forName("UTF-8"));
      return pageTemplate.replaceAll("%IDE_NAME", ApplicationNamesInfo.getInstance().getFullProductName());
    } catch (IOException e) {
      return defaultMessage;
    }
  }

  private static void startServer() {
    boolean isRunning;

    try {
      Spark.port(OAUTH_REDIRECT_PORT);
      isRunning = false;
    } catch (IllegalStateException e) {
      isRunning = true;
    }

    if (!isRunning) {
      Spark.get("/", (request, response) -> {
        String code = request.queryParams("code");

        if (code == null) {
          return null; // Show 404 if request doesn't have `code` query
        }

        CheckiOUser newUser = CheckiOApiController.getInstance().getUser(code);
        if (newUser != null) {
          ApplicationManager.getApplication().getMessageBus().syncPublisher(LOGGED_IN).userLoggedIn(newUser);
          return getOauthResultPage(true);
        }
        return getOauthResultPage(false);
      });
      Spark.awaitInitialization();
    }
  }

  @FunctionalInterface
  public interface CheckioUserLoggedIn {
    void userLoggedIn(CheckiOUser user);
  }
}
