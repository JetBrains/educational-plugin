package com.jetbrains.edu.learning.checker

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task

open class TaskChecker<out T: Task>(@JvmField val task: T,@JvmField val project: Project) {
    open fun onTaskSolved(message: String) {
        ApplicationManager.getApplication()
                .invokeLater { CheckUtils.showTestResultPopUp(message, MessageType.INFO.popupBackground, project) }
    }

    open fun onTaskFailed(message: String) {
        ApplicationManager.getApplication()
                .invokeLater { CheckUtils.showTestResultPopUp(message, MessageType.ERROR.popupBackground, project) }
    }

    open fun check() =
            CheckResult(CheckStatus.Unchecked, "Check for ${task.taskType} task isn't available")

    /**
     * Checks solution for a task on Stepik
     * @return result of a check. If remote check is unsupported returns special instance of check result.
     * @see CheckResult.USE_LOCAL_CHECK
     */
    open fun checkOnRemote(): CheckResult = CheckResult.USE_LOCAL_CHECK

    open fun clearState() {}

    companion object {
        @JvmField val EP_NAME = "Educational.taskChecker"
        @JvmField val LOG = Logger.getInstance(TaskChecker::class.java)
    }
}