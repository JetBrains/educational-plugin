package com.jetbrains.edu.learning.checker

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task

open class TaskChecker<out T : Task>(@JvmField val task: T, @JvmField val project: Project) {
  open fun onTaskSolved(message: String) {
  }

  open fun onTaskFailed(message: String, details: String?) {
  }

  open fun check(indicator: ProgressIndicator) = CheckResult(CheckStatus.Unchecked, "Check for ${task.taskType} task isn't available")

  open fun clearState() {}

  companion object {
    const val EP_NAME = "Educational.taskChecker"
    @JvmField
    val LOG = Logger.getInstance(TaskChecker::class.java)
  }
}