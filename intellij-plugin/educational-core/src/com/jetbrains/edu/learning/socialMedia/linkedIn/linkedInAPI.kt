@file:Suppress("unused")

package com.jetbrains.edu.learning.socialMedia.linkedIn

import com.fasterxml.jackson.annotation.JsonProperty
import com.jetbrains.edu.learning.authUtils.OAuthAccount
import com.jetbrains.edu.learning.courseFormat.UserInfo
import com.jetbrains.edu.learning.socialMedia.linkedIn.LinkedInConnector.Companion.LINKEDIN
import org.jetbrains.annotations.TestOnly

class LinkedInAccount : OAuthAccount<LinkedInUserInfo> {
  @TestOnly
  constructor() : super()

  constructor(tokenExpiresAt: Long) : super(tokenExpiresAt)
  constructor(userInfo: LinkedInUserInfo, tokenExpiresAt: Long) : super(userInfo, tokenExpiresAt)

  override val servicePrefix: String = LINKEDIN

}

class LinkedInUserInfo : UserInfo {

  constructor() : super()

  constructor(id: String, userName: String) {
    this.id = id
    this.name = userName
  }

  @JsonProperty("sub")
  var id: String = ""

  @JsonProperty("email")
  var email: String = ""

  @JsonProperty("name")
  var name: String = ""

  override var isGuest: Boolean = false

  override fun getFullName(): String {
    return name
  }

  override fun toString(): String {
    return getFullName()
  }
}

class LinkedInPostTextBody {
  @JsonProperty("author")
  var author: String = ""

  @JsonProperty("lifecycleState")
  var lifecycleState: String = "PUBLISHED"

  @JsonProperty("specificContent")
  var specificContent: SpecificContent = SpecificContent()

  @JsonProperty("visibility")
  var visibility: Visibility = Visibility()
}

class Visibility {
  @JsonProperty("com.linkedin.ugc.MemberNetworkVisibility")
  var memberNetworkVisibility = "PUBLIC"
}

class SpecificContent {
  @JsonProperty("com.linkedin.ugc.ShareContent")
  var shareContent: ShareContent = ShareContent()
}

class ShareContent {
  @JsonProperty("shareCommentary")
  var shareCommentary: ShareCommentary = ShareCommentary()

  @JsonProperty("shareMediaCategory")
  var shareMediaCategory = "IMAGE"

  @JsonProperty("media")
  var media = arrayOf(Media())
}

class ShareCommentary {
  @JsonProperty("text")
  var text: String = ""
}

class Media {
  @JsonProperty("status")
  var status = "READY"

  @JsonProperty("media")
  var media = ""

  @JsonProperty("title")
  var title = Title()

  @JsonProperty("description")
  var description = Description()
}

class Description {
  @JsonProperty("text")
  var text = ""
}

class Title {
  @JsonProperty("text")
  var text = "JetBrains Academy"
}

class GetMediaUploadLink {
  @JsonProperty("registerUploadRequest")
  var registerUploadRequest = RegisterUploadRequest()
}

class RegisterUploadRequest {
  @JsonProperty("recipes")
  var recipes = arrayOf("urn:li:digitalmediaRecipe:feedshare-image")

  @JsonProperty("owner")
  var owner = ""

  @JsonProperty("serviceRelationships")
  var serviceRelationships = arrayOf(ServiceRelationships())
}

class ServiceRelationships {
  @JsonProperty("relationshipType")
  var relationshipType = "OWNER"

  @JsonProperty("identifier")
  var identifier = "urn:li:userGeneratedContent"

}

class UploadResponse {
  @JsonProperty("value")
  var myGetUploadLinkResponse = GetUploadLinkResponse()
}

class GetUploadLinkResponse {
  @JsonProperty("mediaArtifact")
  var mediaArtifact = ""

  @JsonProperty("uploadMechanism")
  var uploadMechanism = UploadMechanism()

  @JsonProperty("asset")
  var asset = ""

  @JsonProperty("assetRealTimeTopic")
  var assetRealTimeTopic = ""
}

class UploadMechanism {
  @JsonProperty("com.linkedin.digitalmedia.uploading.MediaUploadHttpRequest")
  var mediaUploadHttpRequest = MediaUploadHttpRequest()
}

class MediaUploadHttpRequest {
  @JsonProperty("uploadUrl")
  var uploadUrl = ""

  @JsonProperty("headers")
  var headers = Headers()
}

class Headers {
  @JsonProperty("media-type-family")
  var mediaTypeFamily = ""
}

class ShareMediaContentBody {
  @JsonProperty("author")
  var author = ""

  @JsonProperty("lifecycleState")
  var lifecycleState = "PUBLISHED"

  @JsonProperty("specificContent")
  var specificContent = SpecificContent()

  @JsonProperty("visibility")
  var visibility: Visibility = Visibility()
}
