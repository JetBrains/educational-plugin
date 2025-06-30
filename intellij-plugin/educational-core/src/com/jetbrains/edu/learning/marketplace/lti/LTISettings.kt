package com.jetbrains.edu.learning.marketplace.lti

import com.intellij.openapi.components.BaseState

class LTISettings : BaseState() {
  var launchId: String? by string()
  var lmsDescription: String? by string()
  var onlineService: LTIOnlineService by enum(LTIOnlineService.ALPHA_TEST_2024)
}