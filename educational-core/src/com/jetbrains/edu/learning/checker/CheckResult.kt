package com.jetbrains.edu.learning.checker

import com.jetbrains.edu.learning.EduNames.FAILED_TO_CHECK_URL
import com.jetbrains.edu.learning.EduNames.NO_TESTS_URL
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jetbrains.annotations.Nls
import javax.swing.event.HyperlinkListener

data class CheckResult @JvmOverloads constructor(
  val status: CheckStatus,
  @Nls val message: String = "",
  val details: String? = null,
  val diff: CheckResultDiff? = null,
  val severity: CheckResultSeverity = CheckResultSeverity.Info,
  val hyperlinkListener: HyperlinkListener? = null
) {

  val fullMessage: String get() = if (details == null) message else "$message\n\n$details"

  val isSolved: Boolean get() = status == CheckStatus.Solved

  companion object {
    @JvmField
    val NO_LOCAL_CHECK = CheckResult(CheckStatus.Unchecked, EduCoreBundle.message("check.result.local.check.unavailable"))
    @JvmField
    val LOGIN_NEEDED = CheckResult(CheckStatus.Unchecked, CheckUtils.LOGIN_NEEDED_MESSAGE)
    @JvmField
    val CONNECTION_FAILED = CheckResult(CheckStatus.Unchecked, EduCoreBundle.message("check.result.connection.failed"))
    @JvmField
    val SOLVED = CheckResult(CheckStatus.Solved)
    @JvmField
    val CANCELED = CheckResult(CheckStatus.Unchecked, EduCoreBundle.message("check.result.canceled"))
    @JvmField
    val UNCHECKED = CheckResult(CheckStatus.Unchecked)

    val noTestsRun: CheckResult
      get() = CheckResult(
        CheckStatus.Unchecked,
        EduCoreBundle.message("gluing.dot", EduCoreBundle.message("check.no.tests"),
                              EduCoreBundle.message("help.use.guide", NO_TESTS_URL))
      )

    @JvmStatic
    val failedToCheck: CheckResult
      get() = CheckResult(
        CheckStatus.Unchecked,
        EduCoreBundle.message("gluing.dot", EduCoreBundle.message("error.failed.to.launch.checking"),
                              EduCoreBundle.message("help.use.guide", FAILED_TO_CHECK_URL))
      )
  }
}

data class CheckResultDiff(val expected: String, val actual: String, val title: String = "")
