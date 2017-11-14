package com.jetbrains.edu.learning.checker

import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.CheckUtils.NOT_RUNNABLE_MESSAGE
import com.jetbrains.edu.learning.checker.CheckUtils.createDefaultRunConfiguration
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask

class TheoryTaskChecker : TaskChecker() {
    override fun isAccepted(task: Task) = task is TheoryTask

    override fun onTaskSolved(task: Task, project: Project, message: String) {}

    override fun check(task: Task, project: Project): CheckResult {
        val configuration = createDefaultRunConfiguration(project)
        @Suppress("FoldInitializerAndIfToElvis")
        if (configuration == null) {
            return CheckResult(CheckStatus.Unchecked, NOT_RUNNABLE_MESSAGE)
        }

        ProgramRunnerUtil.executeConfiguration(configuration, DefaultRunExecutor.getRunExecutorInstance())
        return CheckResult(CheckStatus.Solved, "")
    }

    override fun checkOnRemote(task: Task, project: Project) = check(task, project)
}
