package com.jetbrains.edu.learning.socialmedia.twitter

import com.intellij.openapi.components.*
import com.jetbrains.edu.learning.socialmedia.SocialMediaSettings
import java.util.*

@Service
@State(
  name = "StudyTwitterSettings", storages = [
    Storage("academy.socialmedia.settings.xml", roamingType = RoamingType.DISABLED),
    Storage("study_twitter_settings.xml", roamingType = RoamingType.DISABLED, deprecated = true)
  ]
)
class TwitterSettings : SocialMediaSettings<SocialMediaSettings.SocialMediaSettingsState>(SocialMediaSettingsState()) {

  override fun getDefaultUserId(): String {
    return generateNewUserId()
  }

  private fun generateNewUserId(): String {
    val randomId = UUID.randomUUID().toString()
    state.userId = randomId
    return randomId
  }

  companion object {
    fun getInstance(): TwitterSettings = service()
  }
}
