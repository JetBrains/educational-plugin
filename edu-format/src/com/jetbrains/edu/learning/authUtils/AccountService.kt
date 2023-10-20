package com.jetbrains.edu.learning.authUtils

import com.jetbrains.edu.learning.courseFormat.UserInfo


interface AccountService {
  fun getSecret(userName: String?, serviceNameForPasswordSafe: String?): String?

  fun <A : OAuthAccount<UInfo>, UInfo : UserInfo> saveTokens(account: A, tokenInfo: TokenInfo)
}
