package com.jetbrains.edu.learning.checker

import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.checker.CheckUtils.NOT_RUNNABLE_MESSAGE
import com.jetbrains.edu.learning.checker.CheckUtils.createDefaultRunConfiguration
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask

open class TheoryTaskChecker(task: TheoryTask, project: Project) : TaskChecker<TheoryTask>(task, project) {

  override fun check(indicator: ProgressIndicator): CheckResult {
    val configuration = createDefaultRunConfiguration(project)
    if (configuration == null) {
      return CheckResult(CheckStatus.Unchecked, NOT_RUNNABLE_MESSAGE)
    }

    StudyTaskManager.getInstance(project).course?.let {
      runInEdt {
        ProgramRunnerUtil.executeConfiguration(configuration, DefaultRunExecutor.getRunExecutorInstance())
      }
    }
    return CheckResult(CheckStatus.Solved, "")
  }
}
