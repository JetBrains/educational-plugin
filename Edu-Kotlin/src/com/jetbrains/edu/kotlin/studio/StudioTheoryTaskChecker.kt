package com.jetbrains.edu.kotlin.studio

import com.intellij.openapi.project.Project
import com.jetbrains.edu.kotlin.KtTaskChecker.FAILED_TO_LAUNCH
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask

class StudioTheoryTaskChecker : StudioTaskCheckerBase() {
    override fun isAccepted(task: Task) = task is TheoryTask && super.isAccepted(task)

    override fun onTaskSolved(task: Task, project: Project, message: String) {
        // do not show popup
    }

    override fun check(task: Task, project: Project): CheckResult {
        val mainClassName = getMainClassName(project) ?: return FAILED_TO_LAUNCH
        val cmd = generateGradleCommandLine(
                project,
                "${getGradleProjectName(task)}:run",
                "${MAIN_CLASS_PROPERTY_PREFIX}$mainClassName"
        ) ?: return FAILED_TO_LAUNCH

        val output = getProcessOutput(cmd.createProcess(), cmd.commandLineString)
                .postProcessOutput()
                .takeIf { it.isNotBlank() } ?: "<no output>"

        CheckUtils.showOutputToolWindow(project, output)
        return CheckResult(CheckStatus.Solved, "")
    }
}