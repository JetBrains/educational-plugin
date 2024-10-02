package com.jetbrains.edu.learning.socialmedia.linkedIn

import com.intellij.openapi.components.*
import com.jetbrains.edu.learning.socialmedia.SocialMediaSettings

private const val serviceName = "LinkedInSettings"

@Service
@State(name = serviceName, storages = [Storage("academy.socialmedia.settings.xml", roamingType = RoamingType.DISABLED)])
class LinkedInSettings : SocialMediaSettings<LinkedInSettings.LinkedSate>(LinkedSate()) {

  override val name = "LinkedIn"

  var account: LinkedInAccount?
    get() {
      if (userId.isEmpty()) {
        return null
      }
      val userName = state.userName ?: return null
      val linkedInUserInfo = LinkedInUserInfo(userId, userName)
      val linkedInAccount = LinkedInAccount(linkedInUserInfo, expiresIn)
      if (linkedInAccount.isUpToDate()) {
        return linkedInAccount
      }
      else {
        return null
      }
    }
    set(value) {
      if (value == null) {
        userId = ""
        state.userName = ""
        expiresIn = 0
      }
      else {
        userId = value.userInfo.id
        state.userName = value.userInfo.name
        expiresIn = value.tokenExpiresIn
      }
    }
  private var expiresIn: Long
    get() {
      return state.expiresIn
    }
    set(expiresIn) {
      state.expiresIn = expiresIn
    }

  override fun getDefaultUserId(): String {
    return ""
  }

  companion object {
    fun getInstance(): LinkedInSettings = service()
  }

  open class LinkedSate : SocialMediaSettingsState() {
    var userName by string()
    var expiresIn by property(0L)
  }
}