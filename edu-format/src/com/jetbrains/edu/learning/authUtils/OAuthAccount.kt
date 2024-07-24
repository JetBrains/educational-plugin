package com.jetbrains.edu.learning.authUtils

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.IntNode
import com.jetbrains.edu.learning.courseFormat.UserInfo
import com.jetbrains.edu.learning.findService

// Base class for oauth-based accounts
// All user-specific information should be stored in userInfo
abstract class OAuthAccount<UInfo : UserInfo> : Account<UInfo> {

  private val serviceNameForAccessToken get() = "$serviceName access token"
  private val serviceNameForRefreshToken get() = "$serviceName refresh token"

  var tokenExpiresIn: Long = -1

  constructor()

  constructor(tokenExpiresIn: Long) {
    this.tokenExpiresIn = tokenExpiresIn
  }

  constructor(userInfo: UInfo) {
    this.userInfo = userInfo
  }

  constructor(userInfo: UInfo, tokenExpiresIn: Long) {
    this.userInfo = userInfo
    this.tokenExpiresIn = tokenExpiresIn
  }

  override fun isUpToDate() = TokenInfo().apply { expiresIn = tokenExpiresIn }.isUpToDate()

  fun getAccessToken(): String? {
    return getSecret(getUserName(), serviceNameForAccessToken)
  }

  fun getRefreshToken(): String? {
    return getSecret(getUserName(), serviceNameForRefreshToken)
  }

  open fun saveTokens(tokenInfo: TokenInfo) {
    val passwordService = findService(PasswordService::class.java)
    passwordService.saveSecret(getUserName(), serviceNameForAccessToken, tokenInfo.accessToken)
    passwordService.saveSecret(getUserName(), serviceNameForRefreshToken, tokenInfo.refreshToken)
  }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class TokenInfo {
  @JsonProperty("access_token")
  var accessToken: String = ""
  @JsonProperty("refresh_token")
  var refreshToken: String = ""
  @JsonProperty("expires_in")
  @JsonDeserialize(using = ExpiresDeserializer::class)
  var expiresIn: Long = -1

  fun isUpToDate(): Boolean {
    return currentTimeSeconds() < expiresIn - 600 // refresh token before it's expired to avoid failed requests
  }

  private fun currentTimeSeconds(): Long {
    return System.currentTimeMillis() / 1000
  }
}

class ExpiresDeserializer : StdDeserializer<Int>(Int::class.java) {
  override fun deserialize(parser: JsonParser, ctxt: DeserializationContext?): Int {
    val expiresInNode: IntNode = parser.codec.readTree(parser)
    val currentTime: Int = (System.currentTimeMillis() / 1000).toInt()
    val expiresIn = expiresInNode.numberValue() as Int
    return expiresIn + currentTime
  }
}
