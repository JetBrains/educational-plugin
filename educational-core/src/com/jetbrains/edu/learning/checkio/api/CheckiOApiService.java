package com.jetbrains.edu.learning.checkio.api;

import com.intellij.openapi.diagnostic.Logger;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.jetbrains.edu.learning.checkio.api.RetrofitUtils.getResponse;

public abstract class CheckiOApiService {
  private static final Logger LOG = Logger.getInstance(CheckiOApiService.class);
  private final CheckiOApiInterface myApiInterface;

  protected CheckiOApiService(@NotNull CheckiOApiInterface apiInterface) {
    myApiInterface = apiInterface;
  }

  @SuppressWarnings("SameParameterValue")
  private static void log(@NotNull String requestInfo) {
    LOG.info("Executing request: " + requestInfo);
  }

  @NotNull
  public MyResponse<List<CheckiOMission>> getMissionList(@NotNull String token) {
    log("get mission list");
    return getResponse(myApiInterface.getMissionList(token));
  }
}
