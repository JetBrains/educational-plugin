package com.jetbrains.edu.javascript.learning.checker

import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.checker.DefaultCodeExecutor
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus


class JsCodeExecutor : DefaultCodeExecutor() {

  private val syntaxError = listOf("SyntaxError")

  override fun tryToExtractCheckResultError(errorOutput: String): CheckResult? {
    return when {
      syntaxError.any { it in errorOutput } -> CheckResult(CheckStatus.Failed, CheckUtils.SYNTAX_ERROR_MESSAGE, errorOutput)
      else -> null
    }
  }
}
