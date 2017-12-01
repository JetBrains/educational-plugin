package com.jetbrains.edu.learning.checker

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask
import com.jetbrains.edu.learning.stepic.StepicAdaptiveConnector

class CodeTaskChecker(task: CodeTask, project: Project) : TaskChecker<CodeTask>(task, project) {
    override fun onTaskFailed(message: String) {
        super.onTaskFailed("Wrong solution")
        CheckUtils.showTestResultsToolWindow(project, message)
    }

    override fun checkOnRemote(): CheckResult {
        val user = EduSettings.getInstance().user
                ?: return CheckResult(CheckStatus.Unchecked, CheckAction.LOGIN_NEEDED)
        return StepicAdaptiveConnector.checkCodeTask(project, task, user)
    }
}
