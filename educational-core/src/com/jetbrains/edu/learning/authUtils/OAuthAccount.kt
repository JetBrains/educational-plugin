package com.jetbrains.edu.learning.authUtils

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.IntNode
import com.intellij.credentialStore.ACCESS_TO_KEY_CHAIN_DENIED
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.util.NlsSafe
import com.intellij.util.ReflectionUtil
import com.intellij.util.xmlb.SkipDefaultValuesSerializationFilters
import com.intellij.util.xmlb.XmlSerializer
import com.intellij.util.xmlb.annotations.Transient
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jdom.Element

private const val SERVICE_DISPLAY_NAME_PREFIX = "EduTools"

// Base class for oauth-based accounts
// All user-specific information should be stored in userInfo
abstract class OAuthAccount<UserInfo : Any> {
  protected abstract val servicePrefix: String

  private val serviceName @NlsSafe get() = "$servicePrefix Integration"
  private val serviceNameForAccessToken @NlsSafe get() = "$serviceName access token"
  private val serviceNameForRefreshToken @NlsSafe get() = "$serviceName refresh token"

  @field:Transient
  @get:Transient
  lateinit var userInfo: UserInfo
  var tokenExpiresIn: Long = -1

  constructor()

  constructor(tokenExpiresIn: Long) {
    this.tokenExpiresIn = tokenExpiresIn
  }

  fun isTokenUpToDate() = TokenInfo().apply { expiresIn = tokenExpiresIn }.isUpToDate()

  fun serialize(): Element? {
    if (PasswordSafe.instance.isMemoryOnly) {
      return null
    }
    val accountElement = XmlSerializer.serialize(this, SkipDefaultValuesSerializationFilters())

    XmlSerializer.serializeInto(userInfo, accountElement)

    return accountElement
  }

  protected abstract fun getUserName(): String

  fun getAccessToken(): String? {
    return getToken(getUserName(), serviceNameForAccessToken)
  }

  fun getRefreshToken(): String? {
    return getToken(getUserName(), serviceNameForRefreshToken)
  }

  fun saveTokens(tokenInfo: TokenInfo) {
    val userName = getUserName()
    PasswordSafe.instance.set(credentialAttributes(userName, serviceNameForAccessToken), Credentials(userName, tokenInfo.accessToken))
    PasswordSafe.instance.set(credentialAttributes(userName, serviceNameForRefreshToken), Credentials(userName, tokenInfo.refreshToken))
  }
}

fun <Account : OAuthAccount<UserInfo>, UserInfo : Any> deserializeAccount(
  xmlAccount: Element,
  accountClass: Class<Account>,
  userInfoClass: Class<UserInfo>): Account? {

  val account = XmlSerializer.deserialize(xmlAccount, accountClass)

  val userInfo = ReflectionUtil.newInstance(userInfoClass)
  XmlSerializer.deserializeInto(userInfo, xmlAccount)
  account.userInfo = userInfo

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

private fun getToken(userName: String?, serviceNameForPasswordSafe: String?): String? {
  userName ?: return null
  serviceNameForPasswordSafe ?: return null
  val credentials = PasswordSafe.instance.get(credentialAttributes(userName, serviceNameForPasswordSafe)) ?: return null
  if (credentials == ACCESS_TO_KEY_CHAIN_DENIED) {
    val notification = Notification("EduTools", EduCoreBundle.message("notification.tokens.access.denied.title", userName),
                                    EduCoreBundle.message("notification.tokens.access.denied.text"), NotificationType.ERROR)
    notification.notify(null)
    return null
  }
  return credentials.getPasswordAsString()
}

private fun credentialAttributes(userName: String, serviceName: String) =
  CredentialAttributes(generateServiceName("${SERVICE_DISPLAY_NAME_PREFIX} $serviceName", userName))