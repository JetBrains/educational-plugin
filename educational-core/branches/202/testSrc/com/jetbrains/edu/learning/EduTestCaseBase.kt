package com.jetbrains.edu.learning

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.ThrowableRunnable

abstract class EduTestCaseBase : BasePlatformTestCase() {

  override fun runTest() {
    runTestInternal(Unit)
  }

  open fun runTestInternal(context: TestContext) {
    super.runTest()
  }
}
