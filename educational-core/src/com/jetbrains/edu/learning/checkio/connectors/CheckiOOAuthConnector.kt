package com.jetbrains.edu.learning.checkio.connectors

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.intellij.ide.BrowserUtil
import com.intellij.notification.Notifications
import com.intellij.util.xmlb.annotations.Transient
import com.jetbrains.edu.learning.EduLogInListener
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.api.ConnectorUtils
import com.jetbrains.edu.learning.api.EduOAuthConnector
import com.jetbrains.edu.learning.authUtils.OAuthUtils
import com.jetbrains.edu.learning.checkio.account.CheckiOAccount
import com.jetbrains.edu.learning.checkio.account.CheckiOUserInfo
import com.jetbrains.edu.learning.checkio.api.CheckiOEndpoints
import com.jetbrains.edu.learning.checkio.api.exceptions.ApiException
import com.jetbrains.edu.learning.checkio.api.exceptions.NetworkException
import com.jetbrains.edu.learning.checkio.api.executeHandlingCheckiOExceptions
import com.jetbrains.edu.learning.checkio.exceptions.CheckiOLoginRequiredException
import com.jetbrains.edu.learning.checkio.notifications.CheckiONotifications.error
import com.jetbrains.edu.learning.checkio.utils.CheckiONames
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.messages.EduCoreBundle.message
import org.apache.http.client.utils.URIBuilder
import org.jetbrains.ide.RestService
import java.net.URI

abstract class CheckiOOAuthConnector : EduOAuthConnector<CheckiOAccount, CheckiOUserInfo>() {

  @get:Transient
  @set:Transient
  abstract override var account: CheckiOAccount?

  override val authorizationTopicName: String = "Edu.checkioUserLoggedIn"

  override val baseUrl: String = CheckiONames.CHECKIO_OAUTH_HOST

  override val baseOAuthTokenUrl: String = "oauth/token/"

  override val objectMapper: ObjectMapper by lazy {
    ConnectorUtils.createRegisteredMapper(SimpleModule())
  }

  override val oAuthServicePath: String
    get() = "/${RestService.PREFIX}/$serviceName"

  private val checkiOEndpoints: CheckiOEndpoints
    get() = getEndpoints()

  open fun getAccessToken(): String {
    val currentAccount = account ?: throw CheckiOLoginRequiredException()
    if (!isUnitTestMode && !currentAccount.isUpToDate()) {
      refreshTokens()
    }
    return currentAccount.getAccessToken() ?: error("Cannot get access token")
  }

  override fun getUserInfo(account: CheckiOAccount, accessToken: String?): CheckiOUserInfo? {
    return checkiOEndpoints.getUserInfo(accessToken).executeHandlingCheckiOExceptions()
  }

  fun doAuthorize(vararg postLoginActions: Runnable) {
    try {
      if (!OAuthUtils.checkBuiltinPortValid()) return

      val redirectUri = getRedirectUri()
      val oauthLink = getOauthLink(redirectUri)
      initiateAuthorizationListener(*postLoginActions)
      BrowserUtil.browse(oauthLink)
    }
    catch (e: Exception) {
      // IOException is thrown when there're no available ports, in some cases restarting can fix this
      Notifications.Bus.notify(error(
        message("notification.title.failed.to.authorize"),
        null,
        message("notification.content.try.to.restart.ide.and.log.in.again")
      ))
    }
  }

  private fun getOauthLink(oauthRedirectUri: String): URI {
    return URIBuilder(CheckiONames.CHECKIO_OAUTH_URL + "/")
      .addParameter("redirect_uri", oauthRedirectUri)
      .addParameter("response_type", "code")
      .addParameter("client_id", clientId)
      .build()
  }

  private fun initiateAuthorizationListener(vararg postLoginActions: Runnable) =
    reconnectAndSubscribe(object : EduLogInListener {
      override fun userLoggedIn() {
        for (action in postLoginActions) {
          action.run()
        }
      }

      override fun userLoggedOut() {}
    })

  @Synchronized
  override fun login(code: String): Boolean {
    if (account != null) {
      notifyUserLoggedIn()
      return true
    }
    val tokenInfo = retrieveLoginToken(code, getRedirectUri()) ?: return false
    val checkiOAccount = CheckiOAccount(tokenInfo)
    val userInfo =
      try {
        getUserInfo(checkiOAccount, tokenInfo.accessToken)
      }
      catch (e: NetworkException) {
        LOG.warn("Connection failed", e)
        null
      }
      catch (e: ApiException) {
        LOG.warn("Couldn't get user info", e)
        null
      } ?: return false

    checkiOAccount.userInfo = userInfo
    checkiOAccount.saveTokens(tokenInfo)
    account = checkiOAccount
    notifyUserLoggedIn()
    return true
  }

  companion object {
    @JvmStatic
    protected fun getCheckiOServiceName(language: String): String {
      return listOf(EduNames.EDU_PREFIX, CheckiONames.CHECKIO.toLowerCase(), OAUTH_SUFFIX, language).joinToString("/")
    }
  }
}