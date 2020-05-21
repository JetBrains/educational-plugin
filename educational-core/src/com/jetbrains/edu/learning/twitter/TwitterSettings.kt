package com.jetbrains.edu.learning.twitter

import com.intellij.openapi.components.*

@Service
@State(name = "StudyTwitterSettings", storages = [Storage("study_twitter_settings.xml", roamingType = RoamingType.DISABLED)])
class TwitterSettings : SimplePersistentStateComponent<TwitterSettings.State>(State()) {

  fun askToTweet(): Boolean = state.askToTweet

  fun setAskToTweet(askToTweet: Boolean) {
    state.askToTweet = askToTweet
  }

  var accessToken: String
    get() = state.accessToken ?: ""
    set(accessToken) {
      state.accessToken = accessToken
    }

  var tokenSecret: String
    get() = state.tokenSecret ?: ""
    set(tokenSecret) {
      state.tokenSecret = tokenSecret
    }

  companion object {
    @JvmStatic
    fun getInstance(): TwitterSettings = service()
  }

  class State : BaseState() {
    var askToTweet by property(true)
    var accessToken by string()
    var tokenSecret by string()
  }
}
