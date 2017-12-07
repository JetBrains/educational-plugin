package com.jetbrains.edu.learning.checker

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepic.StepicAdaptiveConnector

class CodeTaskChecker : TaskChecker() {
    override fun isAccepted(task: Task) = task is CodeTask

    override fun onTaskFailed(task: Task, project: Project, message: String) {
        super.onTaskFailed(task, project, "Wrong solution")
        CheckUtils.showTestResultsToolWindow(project, message)
    }

    override fun checkOnRemote(task: Task, project: Project): CheckResult {
        val user = EduSettings.getInstance().user
                ?: return CheckResult(CheckStatus.Unchecked, CheckAction.LOGIN_NEEDED)
        return StepicAdaptiveConnector.checkCodeTask(project, task, user)
    }
}
