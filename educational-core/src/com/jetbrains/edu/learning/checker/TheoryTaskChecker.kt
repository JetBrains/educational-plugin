package com.jetbrains.edu.learning.checker

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.CheckUtils.NOT_RUNNABLE_MESSAGE
import com.jetbrains.edu.learning.checker.CheckUtils.createDefaultRunConfiguration
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask

open class TheoryTaskChecker(task: TheoryTask, project: Project) : TaskChecker<TheoryTask>(task, project) {

  override fun check(indicator: ProgressIndicator): CheckResult {
    val configuration = createTestConfiguration()
    if (configuration == null) {
      return CheckResult(CheckStatus.Unchecked, NOT_RUNNABLE_MESSAGE)
    }

    if (!CheckUtils.executeRunConfigurations(project, listOf(configuration), indicator)) {
      LOG.warn("Execution failed")
      return CheckResult.failedToCheck
    }

    return CheckResult.SOLVED
  }

  protected open fun createTestConfiguration(): RunnerAndConfigurationSettings? = createDefaultRunConfiguration(project, task)

  companion object {
    private val LOG = Logger.getInstance(TheoryTaskChecker::class.java)
  }
}
