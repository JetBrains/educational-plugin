package com.jetbrains.edu.learning.checkio.api;

import com.jetbrains.edu.learning.checkio.connectors.ConnectorUtils;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class CheckiOApiService {
  private final CheckiOApiInterface myApiInterface;

  protected CheckiOApiService(@NotNull CheckiOApiInterface apiInterface) {
    myApiInterface = apiInterface;
  }

  @Nullable
  public List<CheckiOMission> getMissionList(@NotNull String token) {
    return ConnectorUtils.getResponseBodyAndUnwrap(myApiInterface.getMissionList(token));
  }
}
