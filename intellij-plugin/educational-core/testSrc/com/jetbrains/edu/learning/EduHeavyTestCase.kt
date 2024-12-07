package com.jetbrains.edu.learning

import com.intellij.testFramework.HeavyPlatformTestCase
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
abstract class EduHeavyTestCase : HeavyPlatformTestCase() {

  override fun setUp() {
    super.setUp()
    EduTestServiceStateHelper.restoreState(null)
  }

  override fun tearDown() {
    try {
      EduTestServiceStateHelper.cleanUpState(null)
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }
}
