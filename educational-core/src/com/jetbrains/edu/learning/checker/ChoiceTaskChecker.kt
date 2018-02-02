package com.jetbrains.edu.learning.checker

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.tasks.ChoiceTask
import com.jetbrains.edu.learning.editor.ChoiceVariantsPanel
import com.jetbrains.edu.learning.stepik.StepikAdaptiveConnector

class ChoiceTaskChecker(task: ChoiceTask, project: Project) : TaskChecker<ChoiceTask>(task, project) {
    override fun checkOnRemote(): CheckResult {
        val user = EduSettings.getInstance().user ?: return CheckResult.LOGIN_NEEDED
        return StepikAdaptiveConnector.checkChoiceTask(task, user)
    }

    override fun onTaskFailed(message: String) {
        super.onTaskFailed(message)
        repaintChoicePanel(project, task)
    }

    private fun repaintChoicePanel(project: Project, task: ChoiceTask) {
        val toolWindow = EduUtils.getStudyToolWindow(project)
        if (toolWindow != null) {
            ApplicationManager.getApplication().invokeLater {
                toolWindow.bottomComponent = ChoiceVariantsPanel(task)
            }
        }
    }
}
