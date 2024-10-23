package com.jetbrains.edu.javascript.learning.checker

import com.jetbrains.edu.learning.EduUtilsKt.escapeAnsiText
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.checker.DefaultCodeExecutor
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus


class JsCodeExecutor : DefaultCodeExecutor() {

  private val syntaxError = listOf("SyntaxError")
  private val executionErrors = listOf("RangeError", "ReferenceError", "TypeError")

  override fun tryToExtractCheckResultError(errorOutput: String): CheckResult? =
    when {
      syntaxError.any { it in errorOutput } -> {
        val escapedText = escapeAnsiText(errorOutput)
        CheckResult(CheckStatus.Failed, CheckUtils.SYNTAX_ERROR_MESSAGE, escapedText)
      }

      executionErrors.any { it in errorOutput } -> CheckResult(CheckStatus.Failed, CheckUtils.EXECUTION_ERROR_MESSAGE, errorOutput)
      else -> null
    }
}
