package com.jetbrains.edu.learning.checkio.model;

import com.intellij.util.xmlb.annotations.Property;
import org.jetbrains.annotations.NotNull;

public class CheckiOUserInfo {
  @Property private String myUsername;
  @Property private int myUid;

  @SuppressWarnings("unused") // used for deserialization
  private CheckiOUserInfo() {}

  public CheckiOUserInfo(@NotNull String username, int uid) {
    myUsername = username;
    myUid = uid;
  }

  @NotNull
  public String getUsername() {
    return myUsername;
  }

  public int getUid() {
    return myUid;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || o.getClass() != getClass()) {
      return false;
    }

    CheckiOUserInfo other = (CheckiOUserInfo) o;
    return getUid() == other.getUid();
  }

  @Override
  public int hashCode() {
    return myUid;
  }
}
