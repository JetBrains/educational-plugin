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
class XSettings : SocialMediaSettings<SocialMediaSettings.SocialMediaSettingsState>(SocialMediaSettingsState()) {

  override val name = "X"

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
}
