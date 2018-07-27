package com.jetbrains.edu.learning.checkio.model;

import com.google.gson.annotations.SerializedName;
import com.intellij.util.xmlb.annotations.Property;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class Tokens {
  @Property
  @SerializedName("access_token")
  private String myAccessToken;

  @Property
  @SerializedName("refresh_token")
  private String myRefreshToken;

  @Property
  @SerializedName("expires_in")
  private int myExpiresIn;

  @Property
  private int myReceivingTime;

  private Tokens() { }

  @Nullable
  public String getAccessToken() {
    return myAccessToken;
  }

  @Nullable
  public String getRefreshToken() {
    return myRefreshToken;
  }

  public boolean isUpToDate() {
    return getCurrentTimeSeconds() < getExpiringTimeSeconds();
  }

  public void markAsReceived() {
    myReceivingTime = getCurrentTimeSeconds();
  }

  private int getExpiringTimeSeconds() {
    return myReceivingTime + myExpiresIn;
  }

  private static int getCurrentTimeSeconds() {
    return (int) (System.currentTimeMillis() / 1000);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Tokens tokens = (Tokens)o;
    return myReceivingTime == tokens.myReceivingTime &&
           Objects.equals(myAccessToken, tokens.myAccessToken) &&
           Objects.equals(myRefreshToken, tokens.myRefreshToken);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myAccessToken, myRefreshToken, myReceivingTime);
  }
}
