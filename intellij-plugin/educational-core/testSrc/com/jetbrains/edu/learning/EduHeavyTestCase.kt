package com.jetbrains.edu.learning

import com.intellij.testFramework.HeavyPlatformTestCase
import com.jetbrains.edu.rules.CustomValuesRule
import org.junit.Rule
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
abstract class EduHeavyTestCase : HeavyPlatformTestCase() {

  @Rule
  @JvmField
  val customValuesRule = CustomValuesRule()

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
