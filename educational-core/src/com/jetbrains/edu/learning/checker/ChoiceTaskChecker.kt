package com.jetbrains.edu.learning.checker

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.editor.ChoiceVariantsPanel
import com.jetbrains.edu.learning.stepic.StepicAdaptiveConnector

class ChoiceTaskChecker : TaskChecker() {
    override fun isAccepted(task: Task) = task is ChoiceTask

    override fun checkOnRemote(task: Task, project: Project): CheckResult {
        val user = EduSettings.getInstance().user ?: return CheckResult(CheckStatus.Unchecked, CheckAction.FAILED_CHECK_LAUNCH)
        return StepicAdaptiveConnector.checkChoiceTask(task as ChoiceTask, user)
    }

    override fun onTaskFailed(task: Task, project: Project, message: String) {
        super.onTaskFailed(task, project, message)
        repaintChoicePanel(project, task as ChoiceTask)
    }

    private fun repaintChoicePanel(project: Project, task: ChoiceTask) {
        val toolWindow = EduUtils.getStudyToolWindow(project)
        if (toolWindow != null) {
            toolWindow.bottomComponent = ChoiceVariantsPanel(task)
        }
    }
}
