package com.jetbrains.edu.learning

import org.jetbrains.annotations.TestOnly

/**
 * A common abstraction for services to clean/restore their state in tests.
 *
 * A service may not be disposed and recreated between two test cases,
 * so you may want to reset service state not to affect other test cases.
 * This interface helps to unify such utility methods and avoid exposing implementation details.
 *
 * Consider addition of service class into [com.jetbrains.edu.learning.EduTestServiceStateHelper],
 * if you mark a service with this interface
 */
interface EduTestAware {
  @TestOnly
  fun restoreState() {}
  @TestOnly
  fun cleanUpState() {}
}
