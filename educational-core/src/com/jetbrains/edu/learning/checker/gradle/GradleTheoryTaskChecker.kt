package com.jetbrains.edu.learning.checker.gradle

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.*
import com.jetbrains.edu.learning.checker.CheckResult.FAILED_TO_CHECK
import com.jetbrains.edu.learning.checker.gradle.MAIN_CLASS_PROPERTY_PREFIX
import com.jetbrains.edu.learning.checker.gradle.generateGradleCommandLine
import com.jetbrains.edu.learning.checker.gradle.getGradleProjectName
import com.jetbrains.edu.learning.checker.gradle.getProcessOutput
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask

abstract class GradleTheoryTaskChecker(task: TheoryTask, project: Project) : TheoryTaskChecker(task, project) {
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

    abstract protected fun getMainClassName(project: Project): String?
}
