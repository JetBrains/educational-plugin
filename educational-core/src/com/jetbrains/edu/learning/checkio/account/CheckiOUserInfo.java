package com.jetbrains.edu.learning.checkio.account;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jetbrains.edu.learning.UserInfo;
import org.jetbrains.annotations.NotNull;


public class CheckiOUserInfo implements UserInfo {
  @NotNull
  @JsonProperty("username")
  private String myUsername;

  @JsonProperty("uid")
  private int myUid;

  @SuppressWarnings("unused") // used for deserialization
  private CheckiOUserInfo() {
    myUsername = "";
    myUid = -1;
  }

  @NotNull
  public String getUsername() {
    return myUsername;
  }

  public void setUsername(@NotNull String username) {
    myUsername = username;
  }

  public int getUid() {
    return myUid;
  }

  public void setUid(int uid) {
    myUid = uid;
  }

  @NotNull
  @Override
  public String getFullName() {
    return myUsername;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || o.getClass() != getClass()) {
      return false;
    }

    CheckiOUserInfo other = (CheckiOUserInfo)o;
    return getUid() == other.getUid();
  }

  @Override
  public int hashCode() {
    return myUid;
  }

  @Override
  public String toString() {
    return myUsername;
  }
}
