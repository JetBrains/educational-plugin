package com.jetbrains.edu.learning.checker

import com.intellij.execution.RunnerAndConfigurationSettings
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

    CheckUtils.executeRunConfigurations(project, listOf(configuration), indicator)
    return CheckResult.SOLVED
  }

  protected open fun createTestConfiguration(): RunnerAndConfigurationSettings? = createDefaultRunConfiguration(project)
}
