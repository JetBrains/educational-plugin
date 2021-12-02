package com.jetbrains.edu.learning.checkio.account;

import com.google.gson.annotations.JsonAdapter;
import com.intellij.util.xmlb.annotations.Tag;
import com.jetbrains.edu.learning.checkio.api.adapters.CheckiOUserInfoDeserializer;
import org.jetbrains.annotations.NotNull;

@JsonAdapter(CheckiOUserInfoDeserializer.class)
public class CheckiOUserInfo {
  @NotNull
  @Tag("Username")
  private String myUsername;

  @Tag("Id")
  private int myUid;

  @SuppressWarnings("unused") // used for deserialization
  private CheckiOUserInfo() {
    myUsername = "";
    myUid = -1;
  }

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
