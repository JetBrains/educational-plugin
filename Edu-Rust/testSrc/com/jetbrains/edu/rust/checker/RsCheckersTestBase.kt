package com.jetbrains.edu.rust.checker

import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.checker.CheckersTestBase
import com.jetbrains.edu.rust.RsCourseBuilder
import com.jetbrains.edu.rust.RsProjectSettings
import org.rust.cargo.toolchain.RustToolchain

// This test runs only when Rust toolchain is found.
abstract class RsCheckersTestBase : CheckersTestBase<RsProjectSettings>() {

  private val toolchain = RustToolchain.suggest()

  override val courseBuilder: EduCourseBuilder<RsProjectSettings> = RsCourseBuilder()
  override val projectSettings: RsProjectSettings get() = RsProjectSettings(toolchain)

  override fun runTest() {
    if (toolchain == null) {
      System.err.println("SKIP $name: no Rust toolchain found")
      return
    }
    super.runTest()
  }
}
