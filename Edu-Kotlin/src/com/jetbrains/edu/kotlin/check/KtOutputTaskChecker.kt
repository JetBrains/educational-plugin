package com.jetbrains.edu.kotlin.check

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.jetbrains.edu.kotlin.check.KtTaskChecker.Companion.FAILED_TO_LAUNCH
import com.jetbrains.edu.learning.checker.*
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask

class KtOutputTaskChecker(task: OutputTask, project: Project) : TaskChecker<OutputTask>(task, project) {
    override fun check(): CheckResult {
        val mainClassName = getMainClassName(project) ?: return FAILED_TO_LAUNCH
        val taskName = "${getGradleProjectName(task)}:run"
        val cmd = generateGradleCommandLine(
                project,
                taskName,
                "$MAIN_CLASS_PROPERTY_PREFIX$mainClassName"
        ) ?: return FAILED_TO_LAUNCH

        val gradleOutput = getProcessOutput(cmd.createProcess(), cmd.commandLineString, taskName)
        if (!gradleOutput.isSuccess) {
            return CheckResult(CheckStatus.Failed, gradleOutput.message)
        }

        val output = gradleOutput.message.takeIf { it.isNotBlank() } ?: "<no output>"

        val outputFile = task.getTaskDir(project)
                ?.parent
                ?.findChild("test")
                ?.findChild(OutputTaskChecker.OUTPUT_PATTERN_NAME)
                ?: return FAILED_TO_LAUNCH

        val expectedOutput = VfsUtil.loadText(outputFile).postProcessOutput()
        if (expectedOutput != output) {
            return CheckResult(CheckStatus.Failed, "Expected output:\n$expectedOutput \nActual output:\n$output")
        }

        return CheckResult(CheckStatus.Solved, TestsOutputParser.CONGRATULATIONS)
    }

    override fun clearState() {
        CheckUtils.drawAllPlaceholders(project, task)
    }
}