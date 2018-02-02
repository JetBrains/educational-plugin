package com.jetbrains.edu.learning.checker

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask
import com.jetbrains.edu.learning.stepik.StepikAdaptiveConnector

class CodeTaskChecker(task: CodeTask, project: Project) : TaskChecker<CodeTask>(task, project) {
    override fun onTaskFailed(message: String) {
        super.onTaskFailed("Wrong solution")
        CheckUtils.showTestResultsToolWindow(project, message)
    }

    override fun checkOnRemote(): CheckResult {
        val user = EduSettings.getInstance().user
                ?: return CheckResult.LOGIN_NEEDED
        return StepikAdaptiveConnector.checkCodeTask(project, task, user)
    }
}
