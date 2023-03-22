package com.jetbrains.edu.python.learning.checker

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.checker.DefaultCodeExecutor
import com.jetbrains.edu.learning.codeforces.run.CodeforcesRunConfiguration
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.python.learning.codeforces.PyCodeforcesRunConfiguration

class PyCodeExecutor : DefaultCodeExecutor() {
  override fun createCodeforcesConfiguration(project: Project, factory: ConfigurationFactory): CodeforcesRunConfiguration {
    return PyCodeforcesRunConfiguration(project, factory)
  }

  override fun tryToExtractCheckResultError(errorOutput: String): CheckResult? =
    if (SYNTAX_ERRORS.any { it in errorOutput }) CheckResult(CheckStatus.Failed, CheckUtils.SYNTAX_ERROR_MESSAGE, errorOutput) else null


  companion object {
    private val SYNTAX_ERRORS = listOf("SyntaxError", "IndentationError", "TabError", "NameError")
  }
}
