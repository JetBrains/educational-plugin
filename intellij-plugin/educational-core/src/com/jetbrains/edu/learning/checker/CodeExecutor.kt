package com.jetbrains.edu.learning.checker

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import org.jetbrains.annotations.Nls

/**
 * Interface for code execution classes
 */

interface CodeExecutor {
  /**
   * @param input String to pass to stdin
   * @return possible values
   * - [Ok][com.jetbrains.edu.learning.Ok] - with executed code output
   * - [Err][com.jetbrains.edu.learning.Err] - with error message, which usually proceeds to [CheckStatus.Unchecked][com.jetbrains.edu.learning.courseFormat.CheckStatus.Unchecked]
   */
  fun execute(
    project: Project,
    task: Task,
    indicator: ProgressIndicator,
    input: String? = null
  ): Result<String, CheckResult>

  fun createRunConfiguration(
    project: Project,
    task: Task
  ): RunnerAndConfigurationSettings? = CheckUtils.createDefaultRunConfiguration(project, task)

  fun tryToExtractCheckResultError(errorOutput: String): CheckResult? = null

  companion object {
    fun resultUnchecked(
      msg: @Nls(capitalization = Nls.Capitalization.Sentence) String
    ): Err<CheckResult> = Err(CheckResult(CheckStatus.Unchecked, msg))
  }
}