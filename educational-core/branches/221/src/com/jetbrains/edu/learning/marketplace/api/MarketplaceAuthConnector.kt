package com.jetbrains.edu.learning.marketplace.api

import com.jetbrains.edu.learning.api.EduOAuthCodeFlowConnector
import com.jetbrains.edu.learning.authUtils.OAuthRestService
import com.jetbrains.edu.learning.courseFormat.MarketplaceUserInfo
import com.jetbrains.edu.learning.marketplace.HUB_API_PATH
import com.jetbrains.edu.learning.marketplace.HUB_AUTH_URL
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnectorUtils.EDU_CLIENT_ID
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnectorUtils.EDU_CLIENT_SECRET
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnectorUtils.MARKETPLACE_CLIENT_ID
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnectorUtils.checkIsGuestAndSave
import org.apache.http.client.utils.URIBuilder

abstract class MarketplaceAuthConnector : EduOAuthCodeFlowConnector<MarketplaceAccount, MarketplaceUserInfo>() {

  override val authorizationUrl: String
    get() = URIBuilder(HUB_AUTH_URL)
      .setPath("$HUB_API_PATH/oauth2/auth")
      .addParameter("access_type", "offline")
      .addParameter("client_id", EDU_CLIENT_ID)
      .addParameter("redirect_uri", getRedirectUri())
      .addParameter("response_type", OAuthRestService.CODE_ARGUMENT)
      .addParameter("scope", "openid $EDU_CLIENT_ID $MARKETPLACE_CLIENT_ID")
      .build()
      .toString()


  override val clientId: String = EDU_CLIENT_ID

  override val clientSecret: String = EDU_CLIENT_SECRET

  @Synchronized
  override fun login(code: String): Boolean {
    val hubTokenInfo = retrieveLoginToken(code, getRedirectUri()) ?: return false
    val account = MarketplaceAccount(hubTokenInfo.expiresIn)
    val currentUser = getUserInfo(account, hubTokenInfo.accessToken) ?: return false
    return checkIsGuestAndSave(currentUser, account, hubTokenInfo)
  }
}