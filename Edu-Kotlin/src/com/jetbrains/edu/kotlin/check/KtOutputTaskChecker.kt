package com.jetbrains.edu.kotlin.check

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.jetbrains.edu.kotlin.check.KtTaskChecker.Companion.FAILED_TO_LAUNCH
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.OutputTaskChecker
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.checker.TestsOutputParser
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task

class KtOutputTaskChecker : TaskChecker() {
    override fun isAccepted(task: Task) = task is OutputTask

    override fun check(task: Task, project: Project): CheckResult {
        val mainClassName = getMainClassName(project) ?: return FAILED_TO_LAUNCH
        val cmd = generateGradleCommandLine(
                project,
                "${getGradleProjectName(task)}:run",
                "$MAIN_CLASS_PROPERTY_PREFIX$mainClassName"
        ) ?: return FAILED_TO_LAUNCH

        val output = getProcessOutput(cmd.createProcess(), cmd.commandLineString)
                .postProcessOutput()
                .takeIf { it.isNotBlank() } ?: "<no output>"

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
}