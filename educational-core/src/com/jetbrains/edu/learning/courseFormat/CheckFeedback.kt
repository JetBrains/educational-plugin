package com.jetbrains.edu.learning.courseFormat

import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.CheckResultDiff
import java.util.*

class CheckFeedback {
  val message: String
  val time: Date?
  val expected: String?
  val actual: String?

  @Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
  @JvmOverloads
  constructor(message: String = "", time: Date? = null, expected: String? = null, actual: String? = null) {
    this.message = message
    this.time = time
    this.expected = expected
    this.actual = actual

  }

  constructor(message: String = "", time: Date? = null, checkResult: CheckResult?) {
    this.message = message
    this.time = time
    expected = checkResult?.diff?.expected
    actual = checkResult?.diff?.actual
  }

  fun composeCheckResult(status: CheckStatus): CheckResult {
    if ((expected == null && actual != null) || (expected != null && actual == null)) {
      LOG.warn("Expected/Actual: one value is missing. Second value would be ignored")
    }
    val diff = if (expected != null && actual != null) CheckResultDiff(expected, actual) else null
    return CheckResult(status, message, diff = diff)
  }

  companion object {
    private val LOG = Logger.getInstance(CheckFeedback::class.java)
  }
}
