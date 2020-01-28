package com.jetbrains.edu.rust.slow.checker

import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.setFeatureEnabled
import org.rust.ide.experiments.RsExperiments

fun withCargoBuildToolWindow(action: () -> Unit) {
  val currentValue = isFeatureEnabled(RsExperiments.BUILD_TOOL_WINDOW)
  try {
    setFeatureEnabled(RsExperiments.BUILD_TOOL_WINDOW, true)
    action()
  }
  finally {
    setFeatureEnabled(RsExperiments.BUILD_TOOL_WINDOW, currentValue)
  }
}
