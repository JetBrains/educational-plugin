package com.jetbrains.edu.learning.twitter

import com.intellij.openapi.components.*
import com.jetbrains.edu.learning.isUnitTestMode
import java.util.*

@Service
@State(name = "StudyTwitterSettings", storages = [Storage("study_twitter_settings.xml", roamingType = RoamingType.DISABLED)])
class TwitterSettings : SimplePersistentStateComponent<TwitterSettings.State>(State()) {

  var askToTweet: Boolean by state::askToTweet

  var userId: String
    get() {
      return state.userId ?: generateNewUserId()
    }
    set(userId) {
      state.userId = userId
    }

  private fun generateNewUserId(): String {
    val randomId = UUID.randomUUID().toString()
    state.userId = randomId
    return randomId
  }

  companion object {
    @JvmStatic
    fun getInstance(): TwitterSettings = service()
  }

  class State : BaseState() {
    var userId by string()
    var askToTweet by property(!isUnitTestMode)
  }
}
