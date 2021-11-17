package com.jetbrains.edu.learning.checker

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task

/**
 * If you add any new public methods here, please do not forget to add it also to
 * @see com.jetbrains.edu.learning.stepik.hyperskill.checker.HyperskillTaskCheckerProvider
 */
open class TaskChecker<out T : Task>(@JvmField val task: T, @JvmField val project: Project) {
  open fun onTaskSolved() {
  }

  open fun onTaskFailed() {
  }

  open fun check(indicator: ProgressIndicator) = CheckResult(CheckStatus.Unchecked, "Check for ${task.itemType} task isn't available")

  open fun clearState() {}

  companion object {
    const val EP_NAME = "Educational.taskChecker"
    @JvmField
    val LOG = Logger.getInstance(TaskChecker::class.java)
  }
}