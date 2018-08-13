package com.jetbrains.edu.learning.checkio.model;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.xmlb.annotations.Property;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class CheckiOAccount {
  private static final Logger LOG = Logger.getInstance(CheckiOAccount.class);

  @Property private CheckiOUserInfo myUserInfo;
  @Property private Tokens myTokens;
  @Property private boolean myLoggedIn;

  @NotNull
  public CheckiOUserInfo getUserInfo() {
    if (!isLoggedIn()) {
      throw new IllegalStateException("Try to get user info when logged out");
    } else if (myUserInfo == null) {
      throw new IllegalStateException("Logged in, but user info are null");
    }
    return myUserInfo;
  }

  @NotNull
  public Tokens getTokens() {
    if (!isLoggedIn()) {
      throw new IllegalStateException("Try to get tokens when logged out");
    } else if (myTokens == null) {
      throw new IllegalStateException("Logged in, but tokens are null");
    }
    return myTokens;
  }

  public boolean isLoggedIn() {
    return myLoggedIn;
  }

  public void logIn(@NotNull CheckiOUserInfo newUserInfo, @NotNull Tokens newTokens) {
    if (isLoggedIn()) {
      LOG.warn("Try to log in when logged in already");
    }

    myUserInfo = newUserInfo;
    myTokens = newTokens;
    myLoggedIn = true;
  }

  public void logOut() {
    if (!isLoggedIn()) {
      LOG.warn("Try to log out when logged out already");
    }

    myUserInfo = null;
    myTokens = null;
    myLoggedIn = false;
  }

  public void updateTokens(@NotNull Tokens newTokens) {
    if (!isLoggedIn()) {
      LOG.warn("Try to update tokens when logged out");
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
