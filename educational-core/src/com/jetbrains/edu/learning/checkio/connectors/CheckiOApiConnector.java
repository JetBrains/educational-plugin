package com.jetbrains.edu.learning.checkio.connectors;

import com.intellij.openapi.diagnostic.Logger;
import com.jetbrains.edu.learning.checkio.api.CheckiOApiService;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class CheckiOApiConnector {
  private static Logger LOG = Logger.getInstance(CheckiOApiConnector.class);

  private final CheckiOApiService myCheckiOApiService;
  private final CheckiOOAuthConnector myOauthConnector;

  protected CheckiOApiConnector(@NotNull CheckiOApiService checkiOApiService, @NotNull CheckiOOAuthConnector oauthConnector) {
    myCheckiOApiService = checkiOApiService;
    myOauthConnector = oauthConnector;
  }

  @Nullable
  public List<CheckiOMission> getMissionList() {
    final String accessToken = myOauthConnector.getAccessToken();
    if (accessToken == null) {
      // TODO: show message
      return null;
    }
    return myCheckiOApiService.getMissionList(myOauthConnector.getAccessToken());
  }
}
