package com.jetbrains.edu.python.learning.checker

import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.checker.StderrAnalyzer
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus

object PyStderrAnalyzer : StderrAnalyzer {
  private val SYNTAX_ERRORS = listOf("SyntaxError", "IndentationError", "TabError", "NameError")

  override fun tryToGetCheckResult(stderr: String): CheckResult? =
    if (SYNTAX_ERRORS.any { it in stderr }) CheckResult(CheckStatus.Failed, CheckUtils.SYNTAX_ERROR_MESSAGE, stderr) else null
}