package com.jetbrains.edu.learning

import org.jetbrains.annotations.TestOnly

/**
 * A common abstraction for services to clean/restore their state in light tests.
 *
 * A service may not be disposed and recreated between two light test cases,
 * so you may want to reset service state not to affect other test cases.
 * This interface helps to unify such utility methods and avoid exposing implementation details.
 *
 * Consider addition of service class into [com.jetbrains.edu.learning.LightTestServiceStateHelper],
 * if you mark a service with this interface
 */
interface LightTestAware {
  @TestOnly
  fun restoreState() {}
  @TestOnly
  fun cleanUpState() {}
}
