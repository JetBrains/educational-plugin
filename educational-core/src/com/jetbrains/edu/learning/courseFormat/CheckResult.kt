package com.jetbrains.edu.learning.courseFormat

import com.jetbrains.edu.learning.courseFormat.EduFormatNames.FAILED_TO_CHECK_URL
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.LOGIN_NEEDED_MESSAGE
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.NO_TESTS_URL
import org.jetbrains.annotations.Nls

data class CheckResult @JvmOverloads constructor(
  val status: CheckStatus,
  @Nls(capitalization = Nls.Capitalization.Sentence) val message: String = "",
  val details: String? = null,
  val diff: CheckResultDiff? = null,
  val severity: CheckResultSeverity = CheckResultSeverity.Info,
  val hyperlinkAction: (() -> Unit)? = null
) {

  val fullMessage: String get() = if (details == null) message else "$message\n\n$details"

  val isSolved: Boolean get() = status == CheckStatus.Solved

  companion object {
    @JvmField
    val NO_LOCAL_CHECK = CheckResult(CheckStatus.Unchecked, message("check.result.local.check.unavailable"))

    @JvmField
    val LOGIN_NEEDED = CheckResult(CheckStatus.Unchecked, LOGIN_NEEDED_MESSAGE)

    @JvmField
    val CONNECTION_FAILED = CheckResult(CheckStatus.Unchecked, message("check.result.connection.failed"))

    @JvmField
    val SOLVED = CheckResult(CheckStatus.Solved)

    @JvmField
    val CANCELED = CheckResult(CheckStatus.Unchecked, message("check.result.canceled"))

    @JvmField
    val UNCHECKED = CheckResult(CheckStatus.Unchecked)

    val noTestsRun: CheckResult
      get() = CheckResult(
        CheckStatus.Unchecked,
        message("check.no.tests.with.help.guide", NO_TESTS_URL)
      )

    @JvmStatic
    val failedToCheck: CheckResult
      get() = CheckResult(
        CheckStatus.Unchecked,
        message("error.failed.to.launch.checking.with.help.guide", FAILED_TO_CHECK_URL)
      )
  }
}

data class CheckResultDiff(val expected: String, val actual: String, val title: String = "")
