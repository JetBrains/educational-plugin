package com.jetbrains.edu.learning.marketplace.userAgreement

import com.intellij.openapi.components.*

@Service
@State(name = "UserAgreementSettings", storages = [Storage(StoragePathMacros.NON_ROAMABLE_FILE, roamingType = RoamingType.DISABLED)])
class UserAgreementSettings : SimplePersistentStateComponent<UserAgreementSettings.State>(State()) {

  // Don't use property delegation like `var isDialogShown by state::isDialogShown`.
  // It doesn't work because `state` may change but delegation keeps the initial state object
  var isDialogShown: Boolean
    get() = state.isDialogShown
    set(value) {
      state.isDialogShown = value
    }

  companion object {
    fun getInstance(): UserAgreementSettings = service()
  }

  class State : BaseState() {
    var isDialogShown by property(false)
  }
}