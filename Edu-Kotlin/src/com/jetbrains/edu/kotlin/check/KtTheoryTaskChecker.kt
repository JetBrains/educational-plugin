package com.jetbrains.edu.kotlin.check

import com.intellij.openapi.project.Project
import com.jetbrains.edu.kotlin.check.KtTaskChecker.Companion.FAILED_TO_LAUNCH
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask

class KtTheoryTaskChecker : TaskChecker() {
    override fun isAccepted(task: Task) = task is TheoryTask

    override fun onTaskSolved(task: Task, project: Project, message: String) {
        // do not show popup
    }

    override fun check(task: Task, project: Project): CheckResult {
        val mainClassName = getMainClassName(project) ?: return FAILED_TO_LAUNCH
        val taskName = "${getGradleProjectName(task)}:run"
        val cmd = generateGradleCommandLine(
                project,
                taskName,
                "${MAIN_CLASS_PROPERTY_PREFIX}$mainClassName"
        ) ?: return FAILED_TO_LAUNCH

        val gradleOutput = getProcessOutput(cmd.createProcess(), cmd.commandLineString, taskName)
        if (!gradleOutput.isSuccess) {
            return CheckResult(CheckStatus.Failed, gradleOutput.message)
        }

        val output = gradleOutput.message.takeIf { it.isNotBlank() } ?: "<no output>"

        CheckUtils.showOutputToolWindow(project, output)
        return CheckResult(CheckStatus.Solved, "")
    }
}