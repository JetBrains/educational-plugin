package com.jetbrains.edu.socialMedia.linkedIn

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType.INFORMATION
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.api.EduOAuthCodeFlowConnector
import com.jetbrains.edu.learning.authUtils.ConnectorUtils
import com.jetbrains.edu.learning.network.executeHandlingExceptions
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.edu.socialMedia.messages.EduSocialMediaBundle
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.RequestBody.Companion.asRequestBody
import org.apache.http.client.utils.URIBuilder
import java.nio.file.Path

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

  private fun getAPIEndpoint(): LinkedInEndpoints =
    getEndpoints<LinkedInEndpoints>(account, account?.getAccessToken(), baseUrl = LINKEDIN_API_URL)

  private fun getWWWEndpoint(): LinkedInEndpoints =
    getEndpoints<LinkedInEndpoints>(account, account?.getAccessToken(), baseUrl = LINKEDIN_BASE_WWW_URL)

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

  private fun getMediaUploadLink(): GetUploadLinkResponse? {
    val getMediaUploadLink = GetMediaUploadLink()
    getMediaUploadLink.registerUploadRequest.owner = "urn:li:person:${account!!.userInfo.id}"
    val result = getAPIEndpoint().getImageUploadLink(getMediaUploadLink).executeHandlingExceptions()
    return result?.body()?.myGetUploadLinkResponse
  }

  private fun uploadMediaFile(uploadLinkData: GetUploadLinkResponse, imagePath: Path): Boolean {
    val uploadUrl = uploadLinkData.uploadMechanism.mediaUploadHttpRequest.uploadUrl.toHttpUrl()
    val params = uploadUrl.queryParameterNames.associateWith { uploadUrl.queryParameter(it)!! } // value can't be null
    val urlPath = uploadUrl.encodedPath
    val requestBody = imagePath.toFile().asRequestBody()
    return getWWWEndpoint().uploadMedia(requestBody, urlPath, params).executeHandlingExceptions()?.isSuccessful == true
  }

  private fun createPost(uploadLinkData: GetUploadLinkResponse, message: String): String? {
    val shareMediaContentBody = ShareMediaContentBody()
    shareMediaContentBody.author = "urn:li:person:${account!!.userInfo.id}"
    shareMediaContentBody.specificContent.shareContent.media[0].media = uploadLinkData.asset
    shareMediaContentBody.specificContent.shareContent.shareCommentary.text = message
    val response = getAPIEndpoint().postTextWithMedia(shareMediaContentBody).executeHandlingExceptions()
    if (response?.isSuccessful == true) {
      return response.headers()["x-restli-id"]
    }
    else {
      return null
    }
  }

  fun createPostWithMedia(project: Project, message: String, imagePath: Path) {
    val uploadLinkData = getMediaUploadLink() ?: return
    if (uploadMediaFile(uploadLinkData, imagePath)) {
      val postId = createPost(uploadLinkData, message)
      if (postId != null) {
        EduNotificationManager
          .create(INFORMATION, EduSocialMediaBundle.message("social.media.success.notification.title"), EduSocialMediaBundle.message("linkedin.post.posted"))
          .addAction(NotificationAction.createSimpleExpiring(EduSocialMediaBundle.message("social.media.open.in.browser.notification.action.text")) {
            EduBrowser.getInstance().browse("https://www.linkedin.com/feed/update/${postId}")
          })
          .notify(project)
      }
      else {
        EduNotificationManager.showErrorNotification(
          project,
          EduSocialMediaBundle.message("social.media.error.failed.to.post.notification"),
          EduSocialMediaBundle.message("social.media.error.failed.to.post.notification")
        )
      }
    }
  }


  companion object {
    private val CLIENT_ID: String = LinkedInOAuthBundle.value("linkedInClientId")
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