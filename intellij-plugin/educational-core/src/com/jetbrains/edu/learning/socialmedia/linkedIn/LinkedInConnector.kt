package com.jetbrains.edu.learning.socialmedia.linkedIn

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.jetbrains.edu.learning.api.EduOAuthCodeFlowConnector
import com.jetbrains.edu.learning.authUtils.ConnectorUtils
import com.jetbrains.edu.learning.network.executeHandlingExceptions
import org.apache.http.client.utils.URIBuilder

@Service
class LinkedInConnector : EduOAuthCodeFlowConnector<LinkedInAccount, LinkedInUserInfo>() {
  override val authorizationUrlBuilder: URIBuilder
    get() = URIBuilder(LINKEDIN_BASE_WWW_URL)
      .setPath("/oauth/v2/authorization")
      .addParameter(CLIENT_ID_PARAM_NAME, CLIENT_ID)
      .addParameter(GRANT_TYPE, "code")
      .addParameter(REDIRECT_URL, getRedirectUri())
      .addParameter(RESPONSE_TYPE, "code")
      .addParameter(SCOPE, "w_member_social openid profile")

  override val baseOAuthTokenUrl: String
    get() = "/oauth/v2/accessToken"

  override val baseUrl: String = LINKEDIN_BASE_WWW_URL
  override val clientId: String = CLIENT_ID
  override val clientSecret: String = CLIENT_SECRET
  override val objectMapper: ObjectMapper by lazy {
    ConnectorUtils.createRegisteredMapper(SimpleModule())
  }
  override val platformName: String = LINKEDIN

  override var account: LinkedInAccount?
    get() {
      return LinkedInSettings.getInstance().account
    }
    set(account) {
      LinkedInSettings.getInstance().account = account
    }

  override fun getUserInfo(account: LinkedInAccount, accessToken: String?): LinkedInUserInfo? {
    val response =
      getEndpoints<LinkedInEndpoints>(account, accessToken, baseUrl = LINKEDIN_API_URL).getCurrentUserInfo().executeHandlingExceptions()
    return response?.body()
  }

  @Synchronized
  override fun login(code: String): Boolean {
    val tokenInfo = retrieveLoginToken(code, getRedirectUri(), codeVerifierFieldName = CODE_VERIFIER_PARAM_NAME) ?: return false
    val account = LinkedInAccount(tokenInfo.expiresIn)
    val currentUser = getUserInfo(account, tokenInfo.accessToken) ?: return false
    account.userInfo = currentUser
    account.saveTokens(tokenInfo)
    this.account = account
    notifyUserLoggedIn()
    return true
  }

  companion object {
    private val CLIENT_ID: String = LinkedInOAuthBundle.value("linkedInClientId")
    private val CLIENT_SECRET: String = LinkedInOAuthBundle.value("linkedInClientSecret")
    private const val LINKEDIN_API_URL = "https://api.linkedin.com"
    const val LINKEDIN_BASE_WWW_URL = "https://www.linkedin.com"
    const val LINKEDIN = "LinkedIn"

    private const val CLIENT_ID_PARAM_NAME = "client_id"
    private const val CODE_VERIFIER_PARAM_NAME = "state"
    private const val GRANT_TYPE = "grant_type"
    private const val REDIRECT_URL = "redirect_uri"
    private const val RESPONSE_TYPE = "response_type"
    private const val SCOPE = "scope"


    fun getInstance(): LinkedInConnector = service()
  }

}