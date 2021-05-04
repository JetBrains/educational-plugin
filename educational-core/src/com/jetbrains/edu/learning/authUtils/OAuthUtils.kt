package com.jetbrains.edu.learning.authUtils

import com.google.common.collect.ImmutableMap
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.io.StreamUtil
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jetbrains.builtInWebServer.BuiltInServerOptions
import org.jetbrains.ide.BuiltInServerManager
import java.io.IOException
import java.nio.charset.StandardCharsets

object OAuthUtils {
  private const val OAUTH_OK_PAGE = "/oauthResponsePages/okPage.html"
  private const val OAUTH_ERROR_PAGE = "/oauthResponsePages/errorPage.html"
  private const val IDE_NAME = "%IDE_NAME"
  private const val PLATFORM_NAME = "%PLATFORM_NAME"
  private const val ERROR_MESSAGE = "%ERROR_MESSAGE"

  @JvmStatic
  @Throws(IOException::class)
  fun getOkPageContent(platformName: String): String {
    return getPageContent(OAUTH_OK_PAGE, ImmutableMap.of(
      IDE_NAME, ApplicationNamesInfo.getInstance().fullProductName,
      PLATFORM_NAME, platformName
    ))
  }

  @JvmStatic
  @Throws(IOException::class)
  fun getErrorPageContent(platformName: String, errorMessage: String): String {
    return getPageContent(OAUTH_ERROR_PAGE, ImmutableMap.of(
      ERROR_MESSAGE, errorMessage,
      PLATFORM_NAME, platformName
    ))
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
    EduUtils::class.java.getResourceAsStream(pagePath).use { pageTemplateStream ->
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
    return port in defaultPort..defaultPort + 20
  }

  private fun showUnsupportedPortError(port: Int) {
    Messages.showErrorDialog(
      EduCoreBundle.message("error.unsupported.port.message", port.toString(), EduNames.OUTSIDE_OF_KNOWN_PORT_RANGE_URL),
      EduCoreBundle.message("error.unsupported.port.title"))
  }

  object GrantType {
    const val AUTHORIZATION_CODE = "authorization_code"
    const val REFRESH_TOKEN = "refresh_token"
  }
}