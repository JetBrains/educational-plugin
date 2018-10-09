package com.jetbrains.edu.learning.checkio.account;

import com.jetbrains.edu.learning.authUtils.OAuthAccount;
import com.jetbrains.edu.learning.authUtils.TokenInfo;

public class CheckiOAccount extends OAuthAccount<CheckiOUserInfo> {

  @SuppressWarnings("unused") // used for deserialization
  private CheckiOAccount() { }

  public CheckiOAccount(CheckiOUserInfo info, TokenInfo tokens) {
    setTokenInfo(tokens);
    setUserInfo(info);
  }
}
