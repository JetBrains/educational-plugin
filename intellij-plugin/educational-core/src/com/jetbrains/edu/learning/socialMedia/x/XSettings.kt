package com.jetbrains.edu.learning.socialMedia.x

import com.intellij.openapi.components.*
import com.jetbrains.edu.learning.socialMedia.SocialMediaSettings
import java.util.*

@Service
@State(
  name = "StudyXSettings", storages = [
    Storage("academy.social.media.settings.xml", roamingType = RoamingType.DISABLED),
    Storage("study_x_settings.xml", roamingType = RoamingType.DISABLED, deprecated = true)
  ]
)
class XSettings : SocialMediaSettings<XSettings.XSettingsState>(XSettingsState()) {

  override val name = XUtils.PLATFORM_NAME

  var account: XAccount?
    get() {
      val userName = state.userId ?: return null
      val name = state.name ?: return null
      val info = XUserInfo(userName, name)
      return XAccount(info, state.expiresIn)
    }
    set(value) {
      state.name = value?.userInfo?.name
      state.userId = value?.userInfo?.userName
      state.expiresIn = value?.tokenExpiresIn ?: 0
    }

  override fun getDefaultUserId(): String {
    return generateNewUserId()
  }

  private fun generateNewUserId(): String {
    val randomId = UUID.randomUUID().toString()
    state.userId = randomId
    return randomId
  }

  companion object {
    fun getInstance(): XSettings = service()
  }

  class XSettingsState : SocialMediaSettingsState() {
    var name by string()
    var expiresIn by property(0L)
  }
}
