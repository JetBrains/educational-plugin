package com.jetbrains.edu.learning.courseFormat

import java.util.*

data class CheckFeedback(
  var message: String = "",
  val time: Date? = null,
  val expected: String? = null,
  val actual: String? = null,
  val failedTestInfo: EduTestInfo? = null,
  val errorDetails: String? = null
) {
  constructor(time: Date, checkResult: CheckResult) :
    this(checkResult.message, time, checkResult.diff?.expected, checkResult.diff?.actual, checkResult.executedTestsInfo.firstOrNull {
      EduTestInfo.PresentableStatus.getPresentableStatus(it.status) == EduTestInfo.PresentableStatus.FAILED.title
    }, checkResult.details)

  fun toCheckResult(status: CheckStatus): CheckResult {
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
