package com.jetbrains.edu.learning.checkio.controllers;

import com.intellij.ide.BrowserUtil;
import com.intellij.util.messages.Topic;
import com.jetbrains.edu.learning.checkio.CheckiONames;
import com.jetbrains.edu.learning.checkio.model.CheckiOUser;
import org.apache.http.client.utils.URIBuilder;
import org.jetbrains.ide.BuiltInServerManager;

import java.net.URI;
import java.net.URISyntaxException;

public class CheckiOAuthorizationController {
  public static final Topic<CheckioUserLoggedIn> LOGGED_IN = Topic.create("Edu.logInCheckiOUSer", CheckioUserLoggedIn.class);

  public static void doAuthorize() {
    BrowserUtil.browse(getOauthLink());
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
    int port = BuiltInServerManager.getInstance().getPort();
    return CheckiONames.EDU_CHECKIO_OAUTH_HOST + ":" + port + CheckiONames.EDU_CHECKIO_OAUTH_SERVICE;
  }

  @FunctionalInterface
  public interface CheckioUserLoggedIn {
    void userLoggedIn(CheckiOUser user);
  }
}
