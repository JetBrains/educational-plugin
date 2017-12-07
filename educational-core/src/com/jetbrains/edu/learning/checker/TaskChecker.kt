package com.jetbrains.edu.learning.checker

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task

abstract class TaskChecker {

    abstract fun isAccepted(task: Task): Boolean

    open fun onTaskSolved(task: Task, project: Project, message: String) {
        ApplicationManager.getApplication()
                .invokeLater { CheckUtils.showTestResultPopUp(message, MessageType.INFO.popupBackground, project) }
    }

    open fun onTaskFailed(task: Task, project: Project, message: String) {
        ApplicationManager.getApplication()
                .invokeLater { CheckUtils.showTestResultPopUp(message, MessageType.ERROR.popupBackground, project) }
    }

    open fun check(task: Task, project: Project) =
            CheckResult(CheckStatus.Unchecked, "Check for ${task.taskType} task isn't available")

    /**
     * Checks solution for a task on Stepik
     * @return result of a check. If remote check is unsupported returns special instance of check result.
     * @see CheckResult.USE_LOCAL_CHECK
     */
    open fun checkOnRemote(task: Task, project: Project): CheckResult = CheckResult.USE_LOCAL_CHECK

    open fun clearState(task: Task, project: Project) {}

    companion object {
        @JvmField val EP_NAME = "Educational.taskChecker"
    }
}