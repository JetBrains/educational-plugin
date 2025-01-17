package com.jetbrains.edu.learning.marketplace.lti

import com.intellij.openapi.components.BaseState

class LTISettings : BaseState() {
  var launchId by string()
  var lmsDescription by string()
  var onlineService by enum<LTIOnlineService>(LTIOnlineService.ALPHA_TEST_2024)
}