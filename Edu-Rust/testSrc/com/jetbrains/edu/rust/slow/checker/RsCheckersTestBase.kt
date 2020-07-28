package com.jetbrains.edu.rust.slow.checker

import com.jetbrains.edu.learning.checker.CheckersTestBase
import com.jetbrains.edu.learning.checker.EduCheckerFixture
import com.jetbrains.edu.learning.withFeature
import com.jetbrains.edu.rust.RsProjectSettings
import org.rust.ide.experiments.RsExperiments

// This test runs only when Rust toolchain is found.
abstract class RsCheckersTestBase : CheckersTestBase<RsProjectSettings>() {

  override fun createCheckerFixture(): EduCheckerFixture<RsProjectSettings> = RsCheckerFixture()

  override fun doTest() {
    // Cargo build tool window is not essential here
    // but IntelliJ Rust plugin doesn't dispose console editor without it
    // and tests fail at platform assertion
    withFeature(RsExperiments.BUILD_TOOL_WINDOW, true) { super.doTest() }
  }
}
