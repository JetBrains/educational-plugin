package com.jetbrains.edu.learning.twitter

import com.intellij.openapi.components.*
import com.jetbrains.edu.learning.isUnitTestMode
import java.util.*

@Service
@State(name = "StudyTwitterSettings", storages = [Storage("study_twitter_settings.xml", roamingType = RoamingType.DISABLED)])
class TwitterSettings : SimplePersistentStateComponent<TwitterSettings.State>(State()) {

  fun askToTweet(): Boolean = state.askToTweet

  fun setAskToTweet(askToTweet: Boolean) {
    state.askToTweet = askToTweet
  }

  var userId: String
    get() {
      val idFromStorage = state.userId
      if (idFromStorage == null) {
        val randomId = UUID.randomUUID().toString()
        state.userId = randomId
        return randomId
      }
      return idFromStorage
    }
    set(userId) {
      state.userId = userId
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
