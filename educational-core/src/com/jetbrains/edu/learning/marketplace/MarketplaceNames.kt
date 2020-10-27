package com.jetbrains.edu.learning.marketplace

import com.intellij.util.io.URLUtil
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.authUtils.CustomAuthorizationServer
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import org.jetbrains.ide.BuiltInServerManager

const val MARKETPLACE = "Marketplace"
const val HUB_URL = "https://hub.jetbrains.com"
const val HUB_AUTH_URL = "$HUB_URL/api/rest/"
const val HUB_PROFILE_PATH = "$HUB_URL/users/"

var CLIENT_ID = MarketplaceOAuthBundle.valueOrDefault("marketplaceClientId", "")
var CLIENT_SECRET = MarketplaceOAuthBundle.valueOrDefault("marketplaceClientSecret", "")
val HUB_AUTHORISATION_CODE_URL: String
  get() = "${HUB_AUTH_URL}oauth2/auth?" +
          "response_type=code&redirect_uri=${URLUtil.encodeURIComponent(REDIRECT_URI)}&" +
          "client_id=$CLIENT_ID&scope=$0-0-0-0-0%20$CLIENT_ID&access_type=offline"
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