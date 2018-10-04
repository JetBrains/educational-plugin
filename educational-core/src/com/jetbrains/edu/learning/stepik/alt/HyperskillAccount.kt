package com.jetbrains.edu.learning.stepik.alt

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

class HyperskillAccount {
  var tokenInfo: TokenInfo? = null
  var userInfo: HyperskillUserInfo? = null

  @Suppress("unused") private constructor() // used for deserialization

  constructor(tokenInfo: TokenInfo) {
    this.tokenInfo = tokenInfo
  }

  fun updateTokens(tokenInfo: TokenInfo) {
    this.tokenInfo = tokenInfo
  }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class HyperskillUserInfo {
  var id: Int = -1
  var email: String = ""
  var fullname: String = ""
  var stage: HyperskillStage? = null
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class HyperskillStage(var id: Int = -1, var title: String = "")

@JsonIgnoreProperties(ignoreUnknown = true)
data class HyperskillProject(var id: Int = -1, var title: String = "", var description: String = "")