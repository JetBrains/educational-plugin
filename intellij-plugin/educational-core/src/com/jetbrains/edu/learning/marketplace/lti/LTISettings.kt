package com.jetbrains.edu.learning.marketplace.lti

import com.intellij.openapi.components.BaseState

class LTISettings : BaseState() {
  var launchId by string()
  var lmsDescription by string()
}