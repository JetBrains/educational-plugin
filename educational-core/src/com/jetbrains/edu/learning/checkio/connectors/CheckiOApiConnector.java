package com.jetbrains.edu.learning.checkio.connectors;

import com.jetbrains.edu.learning.checkio.api.CheckiOApiInterface;
import com.jetbrains.edu.learning.checkio.api.exceptions.ApiException;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission;
import com.jetbrains.edu.learning.checkio.exceptions.CheckiOLoginRequiredException;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class CheckiOApiConnector {
  private final CheckiOApiInterface myCheckiOApiInterface;
  private final CheckiOOAuthConnector myOauthConnector;

  protected CheckiOApiConnector(@NotNull CheckiOApiInterface checkiOApiInterface, @NotNull CheckiOOAuthConnector oauthConnector) {
    myCheckiOApiInterface = checkiOApiInterface;
    myOauthConnector = oauthConnector;
  }

  @NotNull
  public List<CheckiOMission> getMissionList() throws CheckiOLoginRequiredException, ApiException {
    final String accessToken = myOauthConnector.getAccessToken();
    return myCheckiOApiInterface.getMissionList(accessToken).execute();
  }

  @NotNull
  public CheckiOOAuthConnector getOAuthConnector() {
    return myOauthConnector;
  }
}
