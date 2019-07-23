package com.jetbrains.edu.jvm.checker

import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.learning.checker.CheckersTestBase
import com.jetbrains.edu.learning.checker.EduCheckerFixture

abstract class JdkCheckerTestBase : CheckersTestBase<JdkProjectSettings>() {
  override fun createCheckerFixture(): EduCheckerFixture<JdkProjectSettings> = JdkCheckerFixture()
}
