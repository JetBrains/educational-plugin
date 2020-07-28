package com.jetbrains.edu.go.slow.checker

import com.jetbrains.edu.go.GoProjectSettings
import com.jetbrains.edu.learning.checker.CheckersTestBase
import com.jetbrains.edu.learning.checker.EduCheckerFixture

// This test runs only when GO_SDK environment variable is defined and points to the valid Go SDK.
abstract class GoCheckersTestBase : CheckersTestBase<GoProjectSettings>() {
  override fun createCheckerFixture(): EduCheckerFixture<GoProjectSettings> = GoCheckerFixture()
}
