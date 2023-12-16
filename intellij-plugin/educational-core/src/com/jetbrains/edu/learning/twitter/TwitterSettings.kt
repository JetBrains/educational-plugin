package com.jetbrains.edu.learning.twitter

import com.intellij.openapi.components.*
import com.jetbrains.edu.learning.isUnitTestMode
import java.util.*

@Service
@State(name = "StudyTwitterSettings", storages = [Storage("study_twitter_settings.xml", roamingType = RoamingType.DISABLED)])
class TwitterSettings : SimplePersistentStateComponent<TwitterSettings.State>(State()) {

  // Don't use property delegation like `var askToTweet by state::askToTweet`.
  // It doesn't work because `state` may change but delegation keeps the initial state object
  var askToTweet: Boolean
    get() = state.askToTweet
    set(value) {
      state.askToTweet = value
    }

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
    fun getInstance(): TwitterSettings = service()
  }

  class State : BaseState() {
    var userId by string()
    var askToTweet by property(!isUnitTestMode)
  }
}
