package com.jetbrains.edu.learning.checkio.api.wrappers;

import com.google.gson.annotations.SerializedName;
import com.jetbrains.edu.learning.checkio.account.CheckiOTokens;

public class CheckiOTokensWrapper implements ResponseWrapper<CheckiOTokens> {
  @SerializedName("access_token") private String myAccessToken;
  @SerializedName("refresh_token") private String myRefreshToken;
  @SerializedName("expires_in") private int myExpiresIn;

  @Override
  public CheckiOTokens unwrap() {
    return new CheckiOTokens(myAccessToken, myRefreshToken, currentTimeSeconds() + myExpiresIn);
  }

  private static long currentTimeSeconds() {
    return System.currentTimeMillis() / 1000;
  }
}
