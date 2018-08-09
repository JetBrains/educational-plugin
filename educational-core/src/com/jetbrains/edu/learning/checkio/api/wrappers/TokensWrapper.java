package com.jetbrains.edu.learning.checkio.api.wrappers;

import com.google.gson.annotations.SerializedName;
import com.jetbrains.edu.learning.checkio.model.Tokens;

public class TokensWrapper implements ResponseWrapper<Tokens> {
  @SerializedName("access_token") private String myAccessToken;
  @SerializedName("refresh_token") private String myRefreshToken;
  @SerializedName("expires_in") private int myExpiresIn;

  @Override
  public Tokens unwrap() {
    return new Tokens(myAccessToken, myRefreshToken, currentTimeSeconds() + myExpiresIn);
  }

  private static long currentTimeSeconds() {
    return System.currentTimeMillis() / 1000;
  }
}
