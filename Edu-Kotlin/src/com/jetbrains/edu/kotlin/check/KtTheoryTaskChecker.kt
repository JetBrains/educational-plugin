package com.jetbrains.edu.kotlin.check

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.CheckResult.FAILED_TO_CHECK
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.checker.TheoryTaskChecker
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask

class KtTheoryTaskChecker(task: TheoryTask, project: Project) : TheoryTaskChecker(task, project) {
    override fun onTaskSolved(message: String) {
        // do not show popup
    }

    override fun check(): CheckResult {
        val mainClassName = getMainClassName(project) ?: return FAILED_TO_CHECK
        val taskName = "${getGradleProjectName(task)}:run"
        val cmd = generateGradleCommandLine(
                project,
                taskName,
                "${MAIN_CLASS_PROPERTY_PREFIX}$mainClassName"
        ) ?: return FAILED_TO_CHECK

        val gradleOutput = getProcessOutput(cmd.createProcess(), cmd.commandLineString, taskName)
        if (!gradleOutput.isSuccess) {
            return CheckResult(CheckStatus.Failed, gradleOutput.message)
        }

        val output = gradleOutput.message.takeIf { it.isNotBlank() } ?: "<no output>"

        CheckUtils.showOutputToolWindow(project, output)
        return CheckResult(CheckStatus.Solved, "")
    }
}