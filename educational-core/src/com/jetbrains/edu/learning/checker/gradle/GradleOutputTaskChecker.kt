package com.jetbrains.edu.learning.checker.gradle

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.CheckResult.FAILED_TO_CHECK
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.checker.OutputTaskChecker
import com.jetbrains.edu.learning.checker.TestsOutputParser
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask

abstract class GradleOutputTaskChecker(task: OutputTask, project: Project) : OutputTaskChecker(task, project) {
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

        val outputFile = task.getTaskDir(project)
                ?.parent
                ?.findChild("test")
                ?.findChild(OUTPUT_PATTERN_NAME)
                ?: return FAILED_TO_CHECK

        val expectedOutput = VfsUtil.loadText(outputFile).postProcessOutput()
        if (expectedOutput != output) {
            return CheckResult(CheckStatus.Failed, "Expected output:\n$expectedOutput \nActual output:\n$output")
        }

        return CheckResult(CheckStatus.Solved, TestsOutputParser.CONGRATULATIONS)
    }

    override fun clearState() {
      CheckUtils.drawAllPlaceholders(project, task)
    }

    abstract protected fun getMainClassName(project: Project): String?
}
