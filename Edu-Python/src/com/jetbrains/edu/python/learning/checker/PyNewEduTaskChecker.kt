package com.jetbrains.edu.python.learning.checker

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.checker.EduTaskCheckerBase
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.getAllTestFiles
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.python.run.PythonRunConfiguration

class PyNewEduTaskChecker(task: EduTask, project: Project) : EduTaskCheckerBase(task, project) {

  override fun createTestConfigurations(): List<RunnerAndConfigurationSettings> {
    // In general, python plugin can create run configuration for a directory
    // but it can skip some test files if they han't proper names
    return task.getAllTestFiles(project)
      .mapNotNull { ConfigurationContext(it).configuration }
      .filter { it.configuration !is PythonRunConfiguration }
  }

  override fun computePossibleErrorResult(stderr: String): CheckResult =
    if (SYNTAX_ERRORS.any { it in stderr }) CheckResult(CheckStatus.Failed, CheckUtils.SYNTAX_ERROR_MESSAGE, stderr) else CheckResult.SOLVED

  companion object {
    private val SYNTAX_ERRORS = listOf("SyntaxError", "IndentationError", "TabError")
  }
}
