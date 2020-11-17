package com.jetbrains.edu.learning.checker

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.codeforces.run.CodeforcesRunConfiguration
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task

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

  fun createRedirectInputConfiguration(project: Project, factory: ConfigurationFactory): CodeforcesRunConfiguration? = null

  fun setInputRedirectFile(inputFile: VirtualFile, configuration: RunConfiguration) {}

  companion object {
    fun resultUnchecked(msg: String): Err<CheckResult> = Err(CheckResult(CheckStatus.Unchecked, msg))
  }
}