package com.jetbrains.edu.learning.authUtils

import com.google.common.collect.ImmutableMap
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.io.StreamUtil
import com.intellij.util.ReflectionUtil
import com.intellij.util.xmlb.SkipDefaultValuesSerializationFilters
import com.intellij.util.xmlb.XmlSerializer
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtilsKt
import com.jetbrains.edu.learning.courseFormat.UserInfo
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.serialization.CompositeSerializationFilter
import com.jetbrains.edu.learning.serialization.TransientFieldSerializationFilter
import org.jdom.Element
import org.jetbrains.builtInWebServer.BuiltInServerOptions
import org.jetbrains.ide.BuiltInServerManager
import java.io.IOException
import java.nio.charset.StandardCharsets

object OAuthUtils {
  private val LOG: Logger = logger<OAuthUtils>()

  private const val SERVICE_DISPLAY_NAME_PREFIX = "EduTools"
  private const val OAUTH_OK_PAGE = "/oauthResponsePages/okPage.html"
  private const val OAUTH_ERROR_PAGE = "/oauthResponsePages/errorPage.html"
  private const val IDE_NAME = "%IDE_NAME"
  private const val PLATFORM_NAME = "%PLATFORM_NAME"
  private const val ERROR_MESSAGE = "%ERROR_MESSAGE"

  @Throws(IOException::class)
  fun getOkPageContent(platformName: String): String {
    return getPageContent(
      OAUTH_OK_PAGE, ImmutableMap.of(
        IDE_NAME, ApplicationNamesInfo.getInstance().fullProductName,
        PLATFORM_NAME, platformName
      )
    )
  }

  @Throws(IOException::class)
  fun getErrorPageContent(platformName: String, errorMessage: String): String {
    return getPageContent(
      OAUTH_ERROR_PAGE, ImmutableMap.of(
        ERROR_MESSAGE, errorMessage,
        PLATFORM_NAME, platformName
      )
    )
  }

  @Throws(IOException::class)
  private fun getPageContent(pagePath: String, replacements: Map<String, String>): String {
    var pageTemplate = getPageTemplate(pagePath)
    for ((key, value) in replacements) {
      pageTemplate = pageTemplate.replace(key.toRegex(), value)
    }
    return pageTemplate
  }

  @Throws(IOException::class)
  private fun getPageTemplate(pagePath: String): String {
    EduUtilsKt::class.java.getResourceAsStream(pagePath).use { pageTemplateStream ->
      return StreamUtil.readText(pageTemplateStream, StandardCharsets.UTF_8)
    }
  }

  fun checkBuiltinPortValid(): Boolean {
    val port = BuiltInServerManager.getInstance().port
    val isPortValid = isBuiltinPortValid(port)
    if (!isPortValid) {
      showUnsupportedPortError(port)
    }
    return isPortValid
  }

  fun isBuiltinPortValid(port: Int): Boolean {
    val defaultPort = BuiltInServerOptions.DEFAULT_PORT

    // 20 port range comes from org.jetbrains.ide.BuiltInServerManagerImplKt.PORTS_COUNT
    val portsRange = defaultPort..defaultPort + 20
    val isValid = port in portsRange
    if (!isValid) {
      LOG.warn("Built-in port $port is not valid, because it's outside of default port range $portsRange")
    }
    return isValid
  }

  private fun showUnsupportedPortError(port: Int) {
    Messages.showErrorDialog(
      EduCoreBundle.message("error.unsupported.port.message", port.toString(), EduNames.OUTSIDE_OF_KNOWN_PORT_RANGE_URL),
      EduCoreBundle.message("error.authorization.error")
    )
  }

  fun credentialAttributes(userName: String, serviceName: String): CredentialAttributes =
    CredentialAttributes(generateServiceName("$SERVICE_DISPLAY_NAME_PREFIX $serviceName", userName))

  object GrantType {
    const val AUTHORIZATION_CODE = "authorization_code"
    const val REFRESH_TOKEN = "refresh_token"
    const val JBA_TOKEN_EXCHANGE = "jba"
  }
}

fun <UInfo : UserInfo> Account<UInfo>.serialize(): Element? {
  if (PasswordSafe.instance.isMemoryOnly) {
    return null
  }
  val serializationFilter = CompositeSerializationFilter(
    TransientFieldSerializationFilter,
    SkipDefaultValuesSerializationFilters()
  )
  // Do we really need this two-step serialization?
  // Probably, it's worth merging account and user info classes into a single one
  // or copying everything from user info to an account object
  val accountElement = XmlSerializer.serialize(this, serializationFilter)
  XmlSerializer.serializeInto(userInfo, accountElement)
  return accountElement
}

fun <UserAccount : Account<UInfo>, UInfo : UserInfo> Element.deserializeAccount(
  accountClass: Class<UserAccount>,
  userInfoClass: Class<UInfo>
): UserAccount {

  val account = XmlSerializer.deserialize(this, accountClass)

  val userInfo = ReflectionUtil.newInstance(userInfoClass)
  XmlSerializer.deserializeInto(userInfo, this)
  account.userInfo = userInfo

  return account
}

fun <OAuthAcc : OAuthAccount<UInfo>, UInfo : UserInfo> Element.deserializeOAuthAccount(
  accountClass: Class<OAuthAcc>,
  userInfoClass: Class<UInfo>
): OAuthAcc? {

  val account = deserializeAccount(accountClass, userInfoClass)

  val tokenInfo = TokenInfo()
  XmlSerializer.deserializeInto(tokenInfo, this)

  if (tokenInfo.accessToken.isNotEmpty()) {
    return null
  }
  return account
}
