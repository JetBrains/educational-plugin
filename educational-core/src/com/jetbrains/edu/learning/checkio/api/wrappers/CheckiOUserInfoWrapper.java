package com.jetbrains.edu.learning.checkio.api.wrappers;

import com.google.gson.annotations.SerializedName;
import com.jetbrains.edu.learning.checkio.account.CheckiOUserInfo;

public class CheckiOUserInfoWrapper implements ResponseWrapper<CheckiOUserInfo> {
  @SerializedName("username") private String myUsername;
  @SerializedName("uid") private int myUid;

  @Override
  public CheckiOUserInfo unwrap() {
    return new CheckiOUserInfo(myUsername, myUid);
  }
}
