package com.jetbrains.edu.learning.checkio.account;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.xmlb.annotations.Property;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class CheckiOAccount {
  private static final Logger LOG = Logger.getInstance(CheckiOAccount.class);

  @Nullable
  @Property
  private CheckiOUserInfo myUserInfo;

  @Nullable
  @Property
  private Tokens myTokens;

  @NotNull
  public CheckiOUserInfo getUserInfo() {
    if (!isLoggedIn()) {
      throw new IllegalStateException("Attempt to get user info when logged out");
    }
    else if (myUserInfo == null) {
      throw new IllegalStateException("Logged in, but user info are null");
    }
    return myUserInfo;
  }

  @NotNull
  public Tokens getTokens() {
    if (!isLoggedIn()) {
      throw new IllegalStateException("Attempt to get tokens when logged out");
    }
    else if (myTokens == null) {
      throw new IllegalStateException("Logged in, but tokens are null");
    }
    return myTokens;
  }

  public boolean isLoggedIn() {
    return myUserInfo == null;
  }

  public void logIn(@NotNull CheckiOUserInfo newUserInfo, @NotNull Tokens newTokens) {
    if (isLoggedIn()) {
      LOG.warn("Attempt to log in when logged in already");
    }

    myUserInfo = newUserInfo;
    myTokens = newTokens;
  }

  public void logOut() {
    if (!isLoggedIn()) {
      LOG.warn("Attempt to log out when logged out already");
    }

    myUserInfo = null;
    myTokens = null;
  }

  public void updateTokens(@NotNull Tokens newTokens) {
    if (!isLoggedIn()) {
      LOG.warn("Attempt to update tokens when logged out");
    }

    myTokens = newTokens;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CheckiOAccount account = (CheckiOAccount)o;
    return isLoggedIn() == account.isLoggedIn() &&
           Objects.equals(myUserInfo, account.myUserInfo);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myUserInfo, isLoggedIn());
  }
}
