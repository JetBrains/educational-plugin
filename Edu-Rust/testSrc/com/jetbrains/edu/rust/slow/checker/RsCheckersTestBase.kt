package com.jetbrains.edu.rust.slow.checker

import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.setFeatureEnabled
import com.jetbrains.edu.rust.RsProjectSettings
import com.jetbrains.edu.slow.checker.CheckersTestBase
import com.jetbrains.edu.slow.checker.EduCheckerFixture
import org.rust.ide.experiments.RsExperiments

// This test runs only when Rust toolchain is found.
abstract class RsCheckersTestBase : CheckersTestBase<RsProjectSettings>() {

  override fun createCheckerFixture(): EduCheckerFixture<RsProjectSettings> = RsCheckerFixture()

  override fun doTest() {
    // Cargo build tool window is not essential here
    // but IntelliJ Rust plugin doesn't dispose console editor without it
    // and tests fail at platform assertion
    withCargoBuildToolWindow { super.doTest() }
  }

  private fun withCargoBuildToolWindow(action: () -> Unit) {
    val currentValue = isFeatureEnabled(RsExperiments.BUILD_TOOL_WINDOW)
    try {
      setFeatureEnabled(RsExperiments.BUILD_TOOL_WINDOW, true)
      action()
    }
    finally {
      setFeatureEnabled(RsExperiments.BUILD_TOOL_WINDOW, currentValue)
    }
  }

}
