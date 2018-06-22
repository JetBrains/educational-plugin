package com.jetbrains.edu.learning.stepik.alt

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

class HyperskillAccount {
  var tokenInfo: TokenInfo? = null
  var userInfo: HyperskillUserInfo? = null

  // used for deserialization
  private constructor() {}

  constructor(tokenInfo: TokenInfo) {
    this.tokenInfo = tokenInfo
  }

  fun updateTokens(tokenInfo: TokenInfo) {
    this.tokenInfo = tokenInfo
  }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class HyperskillUserInfo(var id: Int = -1, var email: String = "", var fullname: String = "")