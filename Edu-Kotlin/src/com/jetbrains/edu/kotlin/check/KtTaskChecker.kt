package com.jetbrains.edu.kotlin.check

import com.intellij.execution.ExecutionException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task

class KtTaskChecker : TaskChecker() {
    companion object {
        @JvmField
        val FAILED_TO_LAUNCH = CheckResult(CheckStatus.Unchecked, CheckAction.FAILED_CHECK_LAUNCH)
    }

    override fun isAccepted(task: Task) = task is EduTask

    override fun check(task: Task, project: Project): CheckResult {
        val taskName = "${getGradleProjectName(task)}:test"
        val cmd = generateGradleCommandLine(
                project,
                taskName
        ) ?: return FAILED_TO_LAUNCH

        return try {
            val output = parseTestsOutput(cmd.createProcess(), cmd.commandLineString, taskName)
            CheckResult(if (output.isSuccess) CheckStatus.Solved else CheckStatus.Failed, output.message)
        } catch (e: ExecutionException) {
            Logger.getInstance(KtTaskChecker::class.java).info(CheckAction.FAILED_CHECK_LAUNCH, e)
            FAILED_TO_LAUNCH
        }
    }

    override fun clearState(task: Task, project: Project) {
        CheckUtils.drawAllPlaceholders(project, task)
    }
}
