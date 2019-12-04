package com.jetbrains.edu.jvm.slow.checker

import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.slow.checker.CheckersTestBase
import com.jetbrains.edu.slow.checker.EduCheckerFixture

abstract class JdkCheckerTestBase : CheckersTestBase<JdkProjectSettings>() {
  override fun createCheckerFixture(): EduCheckerFixture<JdkProjectSettings> = JdkCheckerFixture()
}
