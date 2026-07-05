package com.jetbrains.edu.rust.slow.checker

import com.jetbrains.edu.learning.checker.EduCheckerFixture
import com.jetbrains.edu.rust.environment.RsLanguageEnvironment
import org.rust.cargo.toolchain.RsToolchainBase

class RsCheckerFixture : EduCheckerFixture<RsLanguageEnvironment>() {

  private var toolchain: RsToolchainBase? = null

  override val projectSettings: RsLanguageEnvironment get() = RsLanguageEnvironment.Existing(toolchain!!)

  override fun getSkipTestReason(): String? = if (toolchain == null) "no Rust toolchain found" else super.getSkipTestReason()

  override fun setUp() {
    super.setUp()
    toolchain = RsToolchainBase.suggest()
  }
}
