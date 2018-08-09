package com.jetbrains.edu.learning.checkio.model;

import com.intellij.util.xmlb.annotations.Property;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class Tokens {
  @Property private String myAccessToken;
  @Property private String myRefreshToken;
  @Property private long myExpiringTime;

  @SuppressWarnings("unused") // used for deserialization
  private Tokens() {}

  public Tokens(@NotNull String accessToken, @NotNull String refreshToken, long expiringTime) {
    myAccessToken = accessToken;
    myRefreshToken = refreshToken;
    myExpiringTime = expiringTime;
  }

  @NotNull
  public String getAccessToken() {
    return myAccessToken;
  }

  @NotNull
  public String getRefreshToken() {
    return myRefreshToken;
  }

  public boolean isUpToDate() {
    return currentTimeSeconds() < myExpiringTime - 600; // subtract 10 minutes for avoiding boundary case
  }

  private static long currentTimeSeconds() {
    return System.currentTimeMillis() / 1000;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Tokens tokens = (Tokens)o;
    return Objects.equals(myAccessToken, tokens.myAccessToken) &&
           Objects.equals(myRefreshToken, tokens.myRefreshToken);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myAccessToken, myRefreshToken);
  }
}
