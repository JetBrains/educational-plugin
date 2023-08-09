package com.jetbrains.edu.learning.checkio.connectors

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.api.ConnectorUtils
import com.jetbrains.edu.learning.api.EduOAuthCodeFlowConnector
import com.jetbrains.edu.learning.authUtils.OAuthRestService.Companion.CODE_ARGUMENT
import com.jetbrains.edu.learning.checkio.account.CheckiOAccount
import com.jetbrains.edu.learning.checkio.account.CheckiOUserInfo
import com.jetbrains.edu.learning.checkio.api.CheckiOEndpoints
import com.jetbrains.edu.learning.checkio.api.exceptions.ApiException
import com.jetbrains.edu.learning.checkio.api.exceptions.NetworkException
import com.jetbrains.edu.learning.checkio.api.executeHandlingCheckiOExceptions
import com.jetbrains.edu.learning.checkio.exceptions.CheckiOLoginRequiredException
import com.jetbrains.edu.learning.checkio.utils.CheckiONames.CHECKIO_URL
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.CHECKIO
import com.jetbrains.edu.learning.isUnitTestMode
import org.apache.http.client.utils.URIBuilder
import org.jetbrains.ide.RestService

abstract class CheckiOOAuthConnector : EduOAuthCodeFlowConnector<CheckiOAccount, CheckiOUserInfo>() {
  override val authorizationUrlBuilder: URIBuilder
    get() {
      return URIBuilder(CHECKIO_URL)
        .setPath("/oauth/authorize/")
        .addParameter("client_id", clientId)
        .addParameter("redirect_uri", getRedirectUri())
        .addParameter("response_type", CODE_ARGUMENT)
    }

  override val baseUrl: String = CHECKIO_URL

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

  @Synchronized
  override fun login(code: String): Boolean {
    if (account != null) return true
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
    return true
  }

  companion object {
    @JvmStatic
    protected fun getCheckiOServiceName(language: String): String {
      return listOf(EduNames.EDU_PREFIX, CHECKIO.lowercase(), OAUTH_SUFFIX, language).joinToString("/")
    }
  }
}