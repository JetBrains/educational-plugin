package com.jetbrains.edu.learning.checkio.connectors;

import com.jetbrains.edu.learning.checkio.api.CheckiOApiService;
import com.jetbrains.edu.learning.checkio.api.MyResponse;
import com.jetbrains.edu.learning.checkio.api.exceptions.ApiException;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission;
import com.jetbrains.edu.learning.checkio.exceptions.LoginRequiredException;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class CheckiOApiConnector {
  private final CheckiOApiService myCheckiOApiService;
  private final CheckiOOAuthConnector myOauthConnector;

  protected CheckiOApiConnector(@NotNull CheckiOApiService checkiOApiService, @NotNull CheckiOOAuthConnector oauthConnector) {
    myCheckiOApiService = checkiOApiService;
    myOauthConnector = oauthConnector;
  }

  @NotNull
  public CheckiOOAuthConnector getOauthConnector() {
    return myOauthConnector;
  }

  @NotNull
  public List<CheckiOMission> getMissionList() throws LoginRequiredException, ApiException {
    final String accessToken = myOauthConnector.getAccessToken();
    final MyResponse<List<CheckiOMission>> missionListResponse = myCheckiOApiService.getMissionList(accessToken);
    return missionListResponse.get();
  }
}
