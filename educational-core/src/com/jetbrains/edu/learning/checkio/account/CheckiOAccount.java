package com.jetbrains.edu.learning.checkio.account;

import com.intellij.util.xmlb.annotations.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class CheckiOAccount {
  @Nullable
  @Tag("UserInfo")
  private CheckiOUserInfo myUserInfo;

  @Nullable
  @Tag("CheckiOTokens")
  private CheckiOTokens myTokens;

  @SuppressWarnings("unused") // used for deserialization
  private CheckiOAccount() {}

  public CheckiOAccount(@NotNull CheckiOUserInfo userInfo, @NotNull CheckiOTokens tokens) {
    myUserInfo = userInfo;
    myTokens = tokens;
  }

  @NotNull
  public CheckiOUserInfo getUserInfo() {
    if (myUserInfo == null) {
      throw new IllegalStateException("Logged in, but user info are null");
    }
    return myUserInfo;
  }

  @NotNull
  public CheckiOTokens getTokens() {
    if (myTokens == null) {
      throw new IllegalStateException("Logged in, but tokens are null");
    }
    return myTokens;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CheckiOAccount account = (CheckiOAccount)o;
    return Objects.equals(myUserInfo, account.myUserInfo);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myUserInfo);
  }
}
