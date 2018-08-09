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

  public CheckiOUserInfo getUserInfo() {
    return myUserInfo;
  }

  public Tokens getTokens() {
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
           Objects.equals(getUserInfo(), account.getUserInfo());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getUserInfo(), isLoggedIn());
  }
}
