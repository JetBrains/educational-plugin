package com.jetbrains.edu.cpp.checker

import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.jetbrains.edu.cpp.generateCMakeProjectUniqueName
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask

class CppEduTaskChecker(task: EduTask, project: Project) : TaskChecker<EduTask>(task, project) {
  override fun check(indicator: ProgressIndicator): CheckResult {
    val taskProjectName = generateCMakeProjectUniqueName(task)
    val configuration = RunManager.getInstance(project).findConfigurationByName(taskProjectName)
                        ?: return CheckResult(CheckStatus.Unchecked, "No <code>target</code> to run", needEscape = false)

    runInEdt {
      ProgramRunnerUtil.executeConfiguration(configuration, DefaultRunExecutor.getRunExecutorInstance())
    }
    return CheckResult(CheckStatus.Solved, "")
  }
}