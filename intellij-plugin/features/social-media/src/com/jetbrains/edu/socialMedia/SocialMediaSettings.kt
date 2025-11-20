package com.jetbrains.edu.socialMedia

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.jetbrains.edu.learning.EduTestAware

abstract class SocialMediaSettings<out T : SocialMediaSettings.SocialMediaSettingsState>(state: T) :
  SimplePersistentStateComponent<@UnsafeVariance T>(state), EduTestAware {

  abstract val name: String

  // Don't use property delegation like `var askToTweet by state::askToPost`.
  // It doesn't work because `state` may change but delegation keeps the initial state object
  var askToPost: Boolean
    get() = state.askToPost
    set(value) {
      state.askToPost = value
    }

  var userId: String
    get() {
      return state.userId ?: getDefaultUserId()
    }
    set(userId) {
      state.userId = userId
    }

  abstract fun getDefaultUserId(): String

  open class SocialMediaSettingsState : BaseState() {
    var userId by string()
    var askToPost by property(true)
  }
}
