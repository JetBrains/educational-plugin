package com.jetbrains.edu.learning

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.HeavyPlatformTestCase
import com.intellij.util.ThrowableRunnable

abstract class HeavyPlatformTestCaseBase : HeavyPlatformTestCase() {

  override fun runTestRunnable(testRunnable: ThrowableRunnable<Throwable>) {
    runTestInternal(testRunnable)
  }

  // BACKCOMPAT: 2020.2. Drop it and use `runTestRunnable` directly
  open fun runTestInternal(context: TestContext) {
    super.runTestRunnable(context)
  }

  protected fun createVirtualDir(): VirtualFile = tempDir.createVirtualDir()
}
