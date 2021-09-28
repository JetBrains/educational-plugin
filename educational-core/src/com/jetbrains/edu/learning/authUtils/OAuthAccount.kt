package com.jetbrains.edu.learning.authUtils

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.IntNode
import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.util.NlsSafe
import com.intellij.util.xmlb.XmlSerializer
import org.jdom.Element

// Base class for oauth-based accounts
// All user-specific information should be stored in userInfo
abstract class OAuthAccount<UserInfo : Any> : Account<UserInfo> {

  private val serviceNameForAccessToken @NlsSafe get() = "$serviceName access token"
  private val serviceNameForRefreshToken @NlsSafe get() = "$serviceName refresh token"

  var tokenExpiresIn: Long = -1

  constructor()

  constructor(tokenExpiresIn: Long) {
    this.tokenExpiresIn = tokenExpiresIn
  }

  override fun isUpToDate() = TokenInfo().apply { expiresIn = tokenExpiresIn }.isUpToDate()

  fun getAccessToken(): String? {
    return getSecret(getUserName(), serviceNameForAccessToken)
  }

  fun getRefreshToken(): String? {
    return getSecret(getUserName(), serviceNameForRefreshToken)
  }

  fun saveTokens(tokenInfo: TokenInfo) {
    val userName = getUserName()
    PasswordSafe.instance.set(credentialAttributes(userName, serviceNameForAccessToken), Credentials(userName, tokenInfo.accessToken))
    PasswordSafe.instance.set(credentialAttributes(userName, serviceNameForRefreshToken), Credentials(userName, tokenInfo.refreshToken))
  }
}

fun <OAuthAcc : OAuthAccount<UserInfo>, UserInfo : Any> deserializeOAuthAccount(
  xmlAccount: Element,
  accountClass: Class<OAuthAcc>,
  userInfoClass: Class<UserInfo>): OAuthAcc? {

  val account = deserializeAccount(xmlAccount, accountClass, userInfoClass)

  val tokenInfo = TokenInfo()
  XmlSerializer.deserializeInto(tokenInfo, xmlAccount)

  if (tokenInfo.accessToken.isNotEmpty()) {
    return null
  }
  return account
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

  @JsonIgnore
  var jwtToken: String = ""

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

