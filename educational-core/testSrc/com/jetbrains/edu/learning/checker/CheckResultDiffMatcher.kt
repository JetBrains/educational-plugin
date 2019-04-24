package com.jetbrains.edu.learning.checker

import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher

class CheckResultDiffMatcher(private val expected: CheckResultDiff) : BaseMatcher<CheckResultDiff?>() {
  override fun describeTo(description: Description) {
    description.appendValue(expected)
  }

  override fun matches(actual: Any?): Boolean {
    if (actual !is CheckResultDiff) return false
    return expected.message == actual.message && expected.actual == actual.actual && expected.expected == actual.expected
  }

  companion object {
    @JvmStatic
    fun diff(expected: CheckResultDiff): Matcher<CheckResultDiff?> = CheckResultDiffMatcher(expected)
  }
}
