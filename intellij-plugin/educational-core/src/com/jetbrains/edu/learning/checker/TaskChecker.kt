package com.jetbrains.edu.learning.checker

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle

open class TaskChecker<out T : Task>(val task: T, val project: Project) {
  open fun onTaskSolved() {
  }

  open fun onTaskFailed() {
  }

  open fun check(indicator: ProgressIndicator) =
    CheckResult(CheckStatus.Unchecked, EduCoreBundle.message("check.result.for.task.not.available", task.itemType))

  open fun clearState() {}

  companion object {
    const val EP_NAME = "Educational.taskChecker"

    val LOG = Logger.getInstance(TaskChecker::class.java)
  }
}