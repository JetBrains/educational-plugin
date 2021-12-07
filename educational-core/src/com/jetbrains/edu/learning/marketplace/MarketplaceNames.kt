package com.jetbrains.edu.learning.marketplace

import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.authUtils.CustomAuthorizationServer
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import org.jetbrains.ide.BuiltInServerManager

const val MARKETPLACE = "Marketplace"
const val HUB_AUTH_URL = "https://hub.jetbrains.com/api/rest/"
const val PLUGINS_REPOSITORY_URL = "https://plugins.jetbrains.com"
const val PLUGINS_EDU_DEMO = "https://edu-courses.dev.marketplace.intellij.net"
const val PLUGINS_MASTER_DEMO = "https://master.demo.marketplace.intellij.net"
const val MARKETPLACE_PROFILE_PATH = "$PLUGINS_REPOSITORY_URL/author/me"
const val LICENSE_URL = "https://creativecommons.org/licenses/by-sa/4.0/"
const val MARKETPLACE_PLUGIN_URL = "$PLUGINS_REPOSITORY_URL/plugin"
const val MARKETPLACE_COURSES_HELP = "${MARKETPLACE_PLUGIN_URL}/10081-edutools/docs/courses-at-marketplace.html"
const val JB_VENDOR_NAME = "JetBrains"
const val REVIEWS = "/reviews"
const val GRAZIE_STAGING_URL = "https://stgn.draft.grazie.ai"

private val port = BuiltInServerManager.getInstance().port
private const val OAUTH_SERVICE_PATH = "/api/edu/marketplace/oauth"
val REDIRECT_URI_DEFAULT = "http://localhost:$port$OAUTH_SERVICE_PATH"
val REDIRECT_URI: String
  get() = if (EduUtils.isAndroidStudio()) {
    getCustomServer().handlingUri
  }
  else {
    REDIRECT_URI_DEFAULT
  }

private fun getCustomServer(): CustomAuthorizationServer {
  val startedServer = CustomAuthorizationServer.getServerIfStarted(MARKETPLACE)
  return startedServer ?: createCustomServer()
}

private fun createCustomServer(): CustomAuthorizationServer {
  return CustomAuthorizationServer.create(MARKETPLACE, OAUTH_SERVICE_PATH)
  { code, _ ->
    if (MarketplaceConnector.getInstance().login(code)) null
    else "Failed to login to ${MARKETPLACE}"
  }
}