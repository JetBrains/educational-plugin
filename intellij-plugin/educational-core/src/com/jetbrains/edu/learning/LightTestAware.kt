package com.jetbrains.edu.learning

import org.jetbrains.annotations.TestOnly

/**
 * A common abstraction for services to clean/restore their state in light tests.
 *
 * A service may not be disposed and recreated between two light test cases,
 * so you may want to reset service state not to affect other test cases.
 * This interface helps to unify such utility methods and avoid exposing implementation details.
 */
interface LightTestAware {
  @TestOnly
  fun restoreState() {}
  @TestOnly
  fun cleanUpState() {}
}
