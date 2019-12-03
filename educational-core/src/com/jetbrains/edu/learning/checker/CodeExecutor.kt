package com.jetbrains.edu.learning.checker

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.Result
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
  ): Result<String, String>

  fun createTestConfiguration(
    project: Project,
    task: Task
  ): RunnerAndConfigurationSettings? = CheckUtils.createDefaultRunConfiguration(project)
}