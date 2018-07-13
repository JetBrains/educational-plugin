package com.jetbrains.edu.learning.checkio.model;

import com.google.gson.annotations.SerializedName;
import com.intellij.util.xmlb.annotations.Property;
import com.jetbrains.edu.learning.checkio.api.CheckiOApiController;

public class CheckiOUser {
  @SerializedName("username")
  private String myUsername;

  @SerializedName("email")
  private String myEmail;

  @SerializedName("uid")
  private int myUid;

  @Property
  private Tokens myTokens;

  private CheckiOUser() {
    myUsername = "";
    myEmail = "";
    myUid = -1;
    myTokens = new Tokens();
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

  @SuppressWarnings("unused") // used for deserialization
  public void setUsername(String username) {
    myUsername = username;
  }

  @SuppressWarnings("unused")
  public void setEmail(String email) {
    myEmail = email;
  }


  @SuppressWarnings("unused")
  public void setUid(int uid) {
    myUid = uid;
  }

  public String getAccessToken() {
    if (!myTokens.isUpToDate()) {
      setTokens(CheckiOApiController.getInstance().refreshTokens(myTokens.getRefreshToken()));
    }
    return myTokens == null ? null : myTokens.getAccessToken();
  }

  public void setTokens(Tokens tokens) {
    myTokens = tokens;
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
    int result = myUid;
    result = 31 * result + myUsername.hashCode();
    result = 31 * result + myEmail.hashCode();
    return result;
  }
}
