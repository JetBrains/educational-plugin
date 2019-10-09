package com.jetbrains.edu.rust.checker

import com.jetbrains.edu.learning.checker.CheckersTestBase
import com.jetbrains.edu.rust.RsProjectSettings
import org.rust.cargo.toolchain.RustToolchain

// This test runs only when Rust toolchain is found.
abstract class RsCheckersTestBase : CheckersTestBase<RsProjectSettings>() {

  private val toolchain = RustToolchain.suggest()

  override val projectSettings: RsProjectSettings get() = RsProjectSettings(toolchain)

  override fun runTest() {
    if (toolchain == null) {
      System.err.println("SKIP $name: no Rust toolchain found")
      return
    }
    super.runTest()
  }

  override fun doTest() {
    // Cargo build tool window is not essential here
    // but IntelliJ Rust plugin doesn't dispose console editor without it
    // and tests fail at platform assertion
    withCargoBuildToolWindow { super.doTest() }
  }
}
