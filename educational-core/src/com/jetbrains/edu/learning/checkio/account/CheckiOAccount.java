package com.jetbrains.edu.learning.checkio.account;

import com.jetbrains.edu.learning.OauthAccount;
import com.jetbrains.edu.learning.TokenInfo;

public class CheckiOAccount extends OauthAccount<CheckiOUserInfo> {

  @SuppressWarnings("unused") // used for deserialization
  private CheckiOAccount() { }

  public CheckiOAccount(CheckiOUserInfo info, TokenInfo tokens) {
    setTokenInfo(tokens);
    setUserInfo(info);
  }
}
