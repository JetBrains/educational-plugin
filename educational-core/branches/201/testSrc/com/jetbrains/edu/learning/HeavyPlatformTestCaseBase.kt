package com.jetbrains.edu.learning

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.HeavyPlatformTestCase

abstract class HeavyPlatformTestCaseBase : HeavyPlatformTestCase() {

  override fun runTest() {
    runTestInternal(Unit)
  }

  open fun runTestInternal(context: TestContext) {
    super.runTest()
  }

  @Suppress("UnstableApiUsage")
  protected fun createVirtualDir(): VirtualFile = tempDir.createTempVDir()
}
