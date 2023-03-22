package com.jetbrains.edu.jvm.gradle.checker

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.jetbrains.edu.jvm.gradle.GradleCodeforcesRunConfiguration
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.checker.CheckUtils.COMPILATION_ERRORS
import com.jetbrains.edu.learning.checker.CheckUtils.COMPILATION_FAILED_MESSAGE
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.checker.CodeExecutor
import com.jetbrains.edu.learning.checker.DefaultCodeExecutor
import com.jetbrains.edu.learning.codeforces.run.CodeforcesRunConfiguration
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task

open class GradleCodeExecutor : CodeExecutor {
  override fun execute(project: Project, task: Task, indicator: ProgressIndicator, input: String?): Result<String, CheckResult> =
    when {
      // TODO https://youtrack.jetbrains.com/issue/EDU-3272
      input != null -> DefaultCodeExecutor().execute(project, task, indicator, input)
      else -> runGradleRunTask(project, task, indicator)
    }

  override fun createCodeforcesConfiguration(project: Project, factory: ConfigurationFactory): CodeforcesRunConfiguration {
    return GradleCodeforcesRunConfiguration(project, factory)
  }

  override fun tryToExtractCheckResultError(errorOutput: String): CheckResult? {
    for (error in COMPILATION_ERRORS) {
      if (error in errorOutput) return CheckResult(CheckStatus.Failed, COMPILATION_FAILED_MESSAGE, errorOutput)
    }
    return null
  }
}