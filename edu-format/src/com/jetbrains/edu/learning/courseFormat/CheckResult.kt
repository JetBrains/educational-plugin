package com.jetbrains.edu.learning.courseFormat

import com.jetbrains.edu.learning.courseFormat.EduFormatNames.FAILED_TO_CHECK_URL
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.LOGIN_NEEDED_MESSAGE
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.NO_TESTS_URL
import org.jetbrains.annotations.Nls

data class CheckResult(
  val status: CheckStatus,
  @Nls(capitalization = Nls.Capitalization.Sentence) val message: String = "",
  val details: String? = null,
  val diff: CheckResultDiff? = null,
  val severity: CheckResultSeverity = CheckResultSeverity.Info,
  val hyperlinkAction: (() -> Unit)? = null,
  val executedTestsInfo: List<EduTestInfo> = emptyList()
) {

  val fullMessage: String get() = if (details == null) message else "$message\n\n$details"

  val isSolved: Boolean get() = status == CheckStatus.Solved

  companion object {
    val NO_LOCAL_CHECK = CheckResult(CheckStatus.Unchecked, message("check.result.local.check.unavailable"))
    val LOGIN_NEEDED = CheckResult(CheckStatus.Unchecked, LOGIN_NEEDED_MESSAGE)
    val CONNECTION_FAILED = CheckResult(CheckStatus.Unchecked, message("check.result.connection.failed"))
    val SOLVED = CheckResult(CheckStatus.Solved)
    val CANCELED = CheckResult(CheckStatus.Unchecked, message("check.result.canceled"))
    val UNCHECKED = CheckResult(CheckStatus.Unchecked)

    val noTestsRun: CheckResult
      get() = CheckResult(
        CheckStatus.Unchecked,
        message("check.no.tests.with.help.guide", NO_TESTS_URL)
      )

    val failedToCheck: CheckResult
      get() = CheckResult(
        CheckStatus.Unchecked,
        message("error.failed.to.launch.checking.with.help.guide", FAILED_TO_CHECK_URL)
      )
  }
}

data class CheckResultDiff(val expected: String, val actual: String, val title: String = "")
