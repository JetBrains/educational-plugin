package com.jetbrains.edu.learning.courseFormat

import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.CheckResultDiff
import java.util.*

data class CheckFeedback(
  val message: String = "",
  val time: Date? = null,
  val expected: String? = null,
  val actual: String? = null
) {
  constructor(time: Date, checkResult: CheckResult) :
    this(checkResult.message, time, checkResult.diff?.expected, checkResult.diff?.actual)

  fun toCheckResult(status: CheckStatus): CheckResult {
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
