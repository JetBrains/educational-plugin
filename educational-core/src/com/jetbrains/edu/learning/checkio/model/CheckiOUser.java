package com.jetbrains.edu.learning.checkio.model;

import com.google.gson.annotations.SerializedName;
import com.intellij.util.xmlb.annotations.Property;

public class CheckiOUser {
  @Property
  @SerializedName("username")
  private String myUsername;

  @Property
  @SerializedName("email")
  private String myEmail;

  @Property
  @SerializedName("uid")
  private int myUid;

  private CheckiOUser() {
    myUsername = "";
    myEmail = "";
    myUid = -1;
  }

  public String getUsername() {
    return myUsername;
  }

  public String getEmail() {
    return myEmail;
  }

  public int getUid() {
    return myUid;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || o.getClass() != getClass()) {
      return false;
    }

    CheckiOUser other = (CheckiOUser) o;
    return getUsername().equals(other.getUsername()) &&
           getEmail().equals(other.getEmail()) &&
           getUid() == other.getUid();
  }

  @Override
  public int hashCode() {
    return myUid;
  }
}
