package com.jetbrains.edu.go.slow.checker

import com.jetbrains.edu.go.environment.GoLanguageEnvironment
import com.jetbrains.edu.learning.checker.CheckersTestBase
import com.jetbrains.edu.learning.checker.EduCheckerFixture

// This test runs only when GO_SDK environment variable is defined and points to the valid Go SDK.
abstract class GoCheckersTestBase : CheckersTestBase<GoLanguageEnvironment>() {
  override fun createCheckerFixture(): EduCheckerFixture<GoLanguageEnvironment> = GoCheckerFixture()
}
