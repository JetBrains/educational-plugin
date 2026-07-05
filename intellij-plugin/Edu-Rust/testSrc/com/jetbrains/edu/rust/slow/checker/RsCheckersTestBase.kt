package com.jetbrains.edu.rust.slow.checker

import com.jetbrains.edu.learning.checker.CheckersTestBase
import com.jetbrains.edu.learning.checker.EduCheckerFixture
import com.jetbrains.edu.rust.environment.RsLanguageEnvironment

// This test runs only when Rust toolchain is found.
abstract class RsCheckersTestBase : CheckersTestBase<RsLanguageEnvironment>() {

  override fun createCheckerFixture(): EduCheckerFixture<RsLanguageEnvironment> = RsCheckerFixture()
}
