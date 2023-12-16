package com.jetbrains.edu.learning.checkio.account

import com.intellij.openapi.util.NlsSafe
import com.jetbrains.edu.learning.authUtils.OAuthAccount
import com.jetbrains.edu.learning.authUtils.TokenInfo
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.CHECKIO

class CheckiOAccount : OAuthAccount<CheckiOUserInfo> {
  @Suppress("unused") // used for deserialization
  private constructor()
  constructor(tokens: TokenInfo) : super(tokens.expiresIn)

  @Suppress("UnstableApiUsage")
  override val servicePrefix: @NlsSafe String
    get() = CHECKIO
}
