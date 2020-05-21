package com.jetbrains.edu.learning.twitter

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

@State(name = "KotlinStudyTwitterSettings", storages = [Storage("kotlin_study_twitter_settings.xml")])
class TwitterSettings : PersistentStateComponent<TwitterSettings.State> {
  private var myState = State()

  class State {
    var askToTweet = true
    var accessToken = ""
    var tokenSecret = ""
  }

  override fun getState(): State? {
    return myState
  }

  override fun loadState(state: State) {
    myState = state
  }

  fun askToTweet(): Boolean {
    return myState.askToTweet
  }

  fun setAskToTweet(askToTweet: Boolean) {
    myState.askToTweet = askToTweet
  }

  var accessToken: String
    get() = myState.accessToken
    set(accessToken) {
      myState.accessToken = accessToken
    }

  var tokenSecret: String
    get() = myState.tokenSecret
    set(tokenSecret) {
      myState.tokenSecret = tokenSecret
    }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): TwitterSettings {
      return ServiceManager.getService(project, TwitterSettings::class.java)
    }
  }
}