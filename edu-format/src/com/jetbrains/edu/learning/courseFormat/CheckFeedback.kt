package com.jetbrains.edu.learning.courseFormat

import java.util.*

data class CheckFeedback(
  val failedTestInfo: EduTestInfo? = null,
  val time: Date? = null
) {
  var message: String = ""
    get() = field.ifBlank { failedTestInfo?.message.orEmpty() }

  var expected: String? = null
    get() = field ?: failedTestInfo?.checkResultDiff?.expected

  var actual: String? = null
    get() = field ?: failedTestInfo?.checkResultDiff?.actual

  // backward compatibility
  constructor(message: String, time: Date? = null, expected: String? = null, actual: String? = null) : this(time = time) {
    this.message = message
    this.expected = expected
    this.actual = actual
  }

  // backward compatibility
  constructor(time: Date, checkResult: CheckResult) : this(checkResult.executedTestsInfo.firstOrNull(), time) {
    this.expected = checkResult.diff?.expected
    this.actual = checkResult.diff?.actual
    this.message = checkResult.message
  }

  fun toCheckResult(status: CheckStatus): CheckResult {
    val expected = expected
    val actual = actual
    if ((expected == null && actual != null) || (expected != null && actual == null)) {
      LOG.warning("Expected/Actual: one value is missing. Second value would be ignored")
    }
    val diff = if (expected != null && actual != null) CheckResultDiff(expected, actual) else null
    return CheckResult(status, message, diff = diff)
  }

  companion object {
    private val LOG = logger<CheckFeedback>()
  }
}
