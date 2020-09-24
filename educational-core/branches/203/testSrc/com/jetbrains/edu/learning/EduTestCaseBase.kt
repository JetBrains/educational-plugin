package com.jetbrains.edu.learning

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.ThrowableRunnable

abstract class EduTestCaseBase : BasePlatformTestCase() {

  override fun runTestRunnable(testRunnable: ThrowableRunnable<Throwable>) {
    runTestInternal(testRunnable)
  }

  // BACKCOMPAT: 2020.2. Drop it and use `runTestRunnable` directly
  open fun runTestInternal(context: TestContext) {
    super.runTestRunnable(context)
  }
}
