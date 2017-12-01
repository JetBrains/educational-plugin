package com.jetbrains.edu.learning.checker

import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.CheckUtils.NOT_RUNNABLE_MESSAGE
import com.jetbrains.edu.learning.checker.CheckUtils.createDefaultRunConfiguration
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask

class TheoryTaskChecker(task: TheoryTask, project: Project) : TaskChecker<TheoryTask>(task, project) {
    override fun onTaskSolved(message: String) {}

    override fun check(): CheckResult {
        val configuration = createDefaultRunConfiguration(project)
        @Suppress("FoldInitializerAndIfToElvis")
        if (configuration == null) {
            return CheckResult(CheckStatus.Unchecked, NOT_RUNNABLE_MESSAGE)
        }

        ProgramRunnerUtil.executeConfiguration(configuration, DefaultRunExecutor.getRunExecutorInstance())
        return CheckResult(CheckStatus.Solved, "")
    }

    override fun checkOnRemote() = check()
}
